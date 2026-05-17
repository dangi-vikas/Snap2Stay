package com.marriott.codefest.snap2stay.visualsearchapi.search;

import com.marriott.codefest.snap2stay.visualsearchapi.domain.IndexedImage;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.ScoredImage;
import com.marriott.codefest.snap2stay.visualsearchapi.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Combines visual k-NN with BM25 over indexed captions and tags.
 *
 * <p>This is the senior engineer's hybrid retrieval idea: pure vector matching
 * gets you "looks similar"; adding BM25 over auto-generated tags acts as a
 * category sanity check.
 *
 * <p>Score formula: {@code combined = w_visual * cos + w_text * bm25_norm}
 * with sensible defaults (0.7 / 0.3) tunable via config.
 */
@Component
public class HybridSearch {

    private static final Logger log = LoggerFactory.getLogger(HybridSearch.class);

    private final float visualWeight;
    private final float textWeight;

    public HybridSearch(
            @Value("${snap2stay.search.visualWeight:0.7}") float visualWeight,
            @Value("${snap2stay.search.textWeight:0.3}") float textWeight) {
        this.visualWeight = visualWeight;
        this.textWeight = textWeight;
        log.info("HybridSearch weights: visual={} text={}", visualWeight, textWeight);
    }

    /**
     * Score visual hits with an additional BM25 contribution from query tokens against
     * each hit's tags + caption. Returns the same hits, re-ranked by combined score.
     *
     * @param visualHits  output of the vector store k-NN
     * @param queryTokens tokens derived from the query (auto-tags and caption words)
     * @param allCorpus   the full set of indexed images, used to compute IDFs
     */
    public List<ScoredImage> rerank(List<VectorStore.ScoredHit> visualHits,
                                    List<String> queryTokens,
                                    List<IndexedImage> allCorpus) {
        if (visualHits.isEmpty()) {
            return List.of();
        }

        // Build IDF table from the corpus. Cheap for codefest corpus size.
        Map<String, Double> idf = computeIdf(allCorpus);
        double avgDocLen = avgDocLen(allCorpus);

        // Compute raw BM25 scores
        float[] bm25Raw = new float[visualHits.size()];
        float maxBm25 = 0f;
        for (int i = 0; i < visualHits.size(); i++) {
            bm25Raw[i] = (float) bm25(queryTokens, visualHits.get(i).image(), idf, avgDocLen);
            if (bm25Raw[i] > maxBm25) maxBm25 = bm25Raw[i];
        }
        // Normalize to [0,1]; if no text overlap, all become 0 and we fall back to pure visual.
        float bm25Denom = maxBm25 > 0 ? maxBm25 : 1f;

        List<ScoredImage> out = new ArrayList<>(visualHits.size());
        for (int i = 0; i < visualHits.size(); i++) {
            VectorStore.ScoredHit hit = visualHits.get(i);
            float visual = clamp01(hit.cosineScore());
            float text = bm25Raw[i] / bm25Denom;
            float combined = visualWeight * visual + textWeight * text;
            out.add(new ScoredImage(hit.image(), visual, text, combined));
        }
        out.sort(null);  // ScoredImage implements Comparable, descending combined score
        return out;
    }

    private static double bm25(List<String> queryTokens, IndexedImage doc, Map<String, Double> idf, double avgDocLen) {
        // BM25 parameters
        double k1 = 1.5;
        double b = 0.75;

        List<String> docTokens = tokenize(doc);
        if (docTokens.isEmpty() || queryTokens.isEmpty()) {
            return 0;
        }
        Map<String, Integer> tf = new HashMap<>();
        for (String t : docTokens) {
            tf.merge(t, 1, Integer::sum);
        }
        double docLen = docTokens.size();

        double score = 0;
        for (String q : queryTokens) {
            int f = tf.getOrDefault(q, 0);
            if (f == 0) continue;
            double termIdf = idf.getOrDefault(q, 0.0);
            double numerator = f * (k1 + 1);
            double denominator = f + k1 * (1 - b + b * (docLen / avgDocLen));
            score += termIdf * (numerator / denominator);
        }
        return score;
    }

    private static Map<String, Double> computeIdf(List<IndexedImage> corpus) {
        Map<String, Integer> df = new HashMap<>();
        for (IndexedImage img : corpus) {
            Set<String> seen = new HashSet<>(tokenize(img));
            for (String tok : seen) {
                df.merge(tok, 1, Integer::sum);
            }
        }
        int n = corpus.size();
        Map<String, Double> idf = new HashMap<>();
        for (var e : df.entrySet()) {
            // Standard BM25 IDF with smoothing
            double v = Math.log(1.0 + (n - e.getValue() + 0.5) / (e.getValue() + 0.5));
            idf.put(e.getKey(), v);
        }
        return idf;
    }

    private static double avgDocLen(List<IndexedImage> corpus) {
        if (corpus.isEmpty()) return 1;
        double total = 0;
        for (IndexedImage img : corpus) total += tokenize(img).size();
        return Math.max(1, total / corpus.size());
    }

    private static List<String> tokenize(IndexedImage img) {
        List<String> out = new ArrayList<>();
        if (img.tags() != null) {
            for (String t : img.tags()) {
                if (t != null && !t.isBlank()) out.add(normalize(t));
            }
        }
        if (img.caption() != null) {
            for (String w : img.caption().split("[\\s,;\\.]+")) {
                if (!w.isBlank()) out.add(normalize(w));
            }
        }
        return out;
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).trim();
    }

    private static float clamp01(float x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }
}
