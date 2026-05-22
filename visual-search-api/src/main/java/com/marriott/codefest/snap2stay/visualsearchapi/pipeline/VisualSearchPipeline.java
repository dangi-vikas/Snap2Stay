package com.marriott.codefest.snap2stay.visualsearchapi.pipeline;

import com.marriott.codefest.snap2stay.visualsearchapi.api.VisualSearchDtos;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.IndexedImage;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.QueryHints;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.ScoredImage;
import com.marriott.codefest.snap2stay.visualsearchapi.embedding.EmbeddingClient;
import com.marriott.codefest.snap2stay.visualsearchapi.embedding.EmbeddingDtos;
import com.marriott.codefest.snap2stay.visualsearchapi.preprocess.ImagePreprocessor;
import com.marriott.codefest.snap2stay.visualsearchapi.search.HybridSearch;
import com.marriott.codefest.snap2stay.visualsearchapi.vectorstore.VectorFilters;
import com.marriott.codefest.snap2stay.visualsearchapi.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The query pipeline orchestrator.
 *
 * <p>Order of operations: preprocess (strip EXIF) -> embed -> hybrid search ->
 * aggregate by property -> availability filter -> rerank -> location expansion.
 */
@Service
public class VisualSearchPipeline {

    private static final Logger log = LoggerFactory.getLogger(VisualSearchPipeline.class);

    private final ImagePreprocessor preprocessor;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final HybridSearch hybrid;
    private final PropertyAggregator aggregator;
    private final AvailabilityStub availability;
    private final LocationExpander locationExpander;

    private final int knnK;
    private final int finalCap;

    public VisualSearchPipeline(ImagePreprocessor preprocessor,
                                EmbeddingClient embeddingClient,
                                VectorStore vectorStore,
                                HybridSearch hybrid,
                                PropertyAggregator aggregator,
                                AvailabilityStub availability,
                                LocationExpander locationExpander,
                                @Value("${snap2stay.search.knnK:200}") int knnK,
                                @Value("${snap2stay.search.finalCap:10}") int finalCap) {
        this.preprocessor = preprocessor;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.hybrid = hybrid;
        this.aggregator = aggregator;
        this.availability = availability;
        this.locationExpander = locationExpander;
        this.knnK = knnK;
        this.finalCap = finalCap;
    }

    public Mono<VisualSearchDtos.VisualSearchResponse> search(byte[] rawImage,
                                                              boolean useImageLocation,
                                                              VisualSearchDtos.SearchFilters filters,
                                                              VisualSearchDtos.DateRange dates) {
        ImagePreprocessor.PreprocessResult pre = preprocessor.preprocess(rawImage, useImageLocation);
        QueryHints hints = pre.queryHints();

        return embeddingClient.embedAndTag(pre.cleanBytes(), "guest.jpg")
                .map(emb -> doSearch(emb, hints, filters, dates))
                .doOnError(e -> log.warn("Embedding call failed: {}", e.toString()));
    }

    private VisualSearchDtos.VisualSearchResponse doSearch(EmbeddingDtos.EmbedAndTagResponse emb,
                                                           QueryHints hints,
                                                           VisualSearchDtos.SearchFilters filters,
                                                           VisualSearchDtos.DateRange dates) {
        VisualSearchDtos.SearchFilters f = filters != null ? filters : VisualSearchDtos.SearchFilters.empty();
        VectorFilters vfilters = new VectorFilters(
                f.brand() == null ? List.of() : f.brand(),
                f.marketCode(),
                f.maxPriceUSD(),
                hints.geo());

        List<VectorStore.ScoredHit> visualHits =
                vectorStore.nearestNeighbors(emb.vectorArray(), knnK, vfilters);

        // Build query tokens from auto-tags + caption words. These drive BM25.
        List<String> queryTokens = new ArrayList<>();
        if (emb.tags() != null) queryTokens.addAll(emb.tags());
        if (emb.caption() != null) {
            for (String w : emb.caption().split("[\\s,;\\.]+")) {
                if (!w.isBlank()) queryTokens.add(w.toLowerCase());
            }
        }

        List<IndexedImage> corpusForIdf = new ArrayList<>(visualHits.size());
        for (var h : visualHits) corpusForIdf.add(h.image());

        List<ScoredImage> scored = hybrid.rerank(visualHits, queryTokens, corpusForIdf);
        List<ScoredImage> aggregated = aggregator.aggregate(scored);

        // Log the top scores to make the threshold tunable with evidence.
        if (log.isInfoEnabled()) {
            int peek = Math.min(5, aggregated.size());
            StringBuilder sb = new StringBuilder("Top aggregated scores: ");
            for (int i = 0; i < peek; i++) {
                ScoredImage s = aggregated.get(i);
                sb.append(s.image().propertyCode())
                  .append("=").append(String.format("%.3f", s.combinedScore()))
                  .append(" (vis=").append(String.format("%.3f", s.visualScore()))
                  .append(", txt=").append(String.format("%.3f", s.textScore()))
                  .append(") ");
            }
            log.info(sb.toString());
        }

        // Filter to confident matches (>= 0.70 combined). To avoid an empty
        // results page on a clearly-best (but sub-threshold) hit — common when
        // the embedding service produces slightly different tags/cosines on a
        // re-uploaded seed image — always keep the single top candidate even
        // if it's below threshold. This guarantees the user sees *something*
        // for any image in the corpus.
        List<ScoredImage> confident = aggregated.stream()
                .filter(s -> s.combinedScore() >= 0.70f)
                .toList();

        if (confident.isEmpty() && !aggregated.isEmpty()) {
            ScoredImage topFallback = aggregated.get(0);
            log.warn("All {} candidates below 0.70 confidence threshold (top={} score={}). " +
                            "Falling back to top match so the rail is non-empty.",
                    aggregated.size(),
                    topFallback.image().propertyCode(),
                    String.format("%.3f", topFallback.combinedScore()));
            confident = List.of(topFallback);
        }

        // Availability filter — only on the top N candidates to keep fan-out bounded
        List<ScoredImage> available = applyAvailability(confident, dates);

        // Cap to final size for the primary rail
        List<ScoredImage> top = available.size() > finalCap ? available.subList(0, finalCap) : available;

        VisualSearchDtos.NearbyGroup nearby = locationExpander.expandAroundTop(top, f, dates);

        // Include debug info for the UI to show what the AI detected
        VisualSearchDtos.DebugInfo debug = new VisualSearchDtos.DebugInfo(emb.tags(), emb.caption());

        return new VisualSearchDtos.VisualSearchResponse(
                top.stream().map(s -> toDto(s, dates)).toList(),
                nearby,
                "qry_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                0L,  // tookMs filled in by the controller via Stopwatch
                debug);
    }

    private List<ScoredImage> applyAvailability(List<ScoredImage> hits, VisualSearchDtos.DateRange dates) {
        if (dates == null) return hits;
        // Keep order but mark availability; the DTO conversion sets the flag.
        // For codefest we don't actually drop unavailable from primary — UI shows the badge.
        return hits;
    }

    private VisualSearchDtos.PropertyMatch toDto(ScoredImage s, VisualSearchDtos.DateRange dates) {
        IndexedImage img = s.image();
        return new VisualSearchDtos.PropertyMatch(
                img.propertyCode(),
                img.name(),
                img.brand(),
                img.city(),
                img.marketCode(),
                s.combinedScore(),
                img.thumbnailUrl(),
                null,
                availability.isAvailable(img.propertyCode(), dates));
    }
}
