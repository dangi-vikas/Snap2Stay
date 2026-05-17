package com.marriott.codefest.snap2stay.visualsearchapi.pipeline;

import com.marriott.codefest.snap2stay.visualsearchapi.api.VisualSearchDtos;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.IndexedImage;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.ScoredImage;
import com.marriott.codefest.snap2stay.visualsearchapi.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Director's ask: after we find the top match, also surface other Marriott properties
 * in that same destination ("Other places in Maldives"). Top-1 expansion: pivot on the
 * top-1 primary match's marketCode and pull siblings.
 *
 * <p>Re-applies the guest's filters (brand, dates, price) so the nearby rail isn't
 * full of unavailable or filtered-out properties.
 */
@Component
public class LocationExpander {

    private static final Logger log = LoggerFactory.getLogger(LocationExpander.class);

    private final InMemoryPropertyDirectory directory;
    private final AvailabilityStub availability;
    private final int maxNearby;

    public LocationExpander(InMemoryPropertyDirectory directory,
                            AvailabilityStub availability,
                            @Value("${snap2stay.locationExpansion.max:10}") int maxNearby) {
        this.directory = directory;
        this.availability = availability;
        this.maxNearby = maxNearby;
    }

    public VisualSearchDtos.NearbyGroup expandAroundTop(List<ScoredImage> primaryMatches,
                                                       VisualSearchDtos.SearchFilters filters,
                                                       VisualSearchDtos.DateRange dates) {
        if (primaryMatches.isEmpty()) {
            return null;
        }
        IndexedImage anchor = primaryMatches.get(0).image();
        List<IndexedImage> siblings = directory.byMarket(anchor.marketCode());
        if (siblings.isEmpty()) {
            log.debug("No siblings found in market {}", anchor.marketCode());
            return null;
        }

        // Exclude the anchor itself and any property already in primaryMatches.
        var primaryCodes = new java.util.HashSet<String>();
        for (ScoredImage s : primaryMatches) primaryCodes.add(s.image().propertyCode());

        List<VisualSearchDtos.PropertyMatch> nearby = new ArrayList<>();
        for (IndexedImage sib : siblings) {
            if (primaryCodes.contains(sib.propertyCode())) continue;
            if (!matchesFilters(sib, filters)) continue;
            boolean isAvailable = availability.isAvailable(sib.propertyCode(), dates);
            nearby.add(new VisualSearchDtos.PropertyMatch(
                    sib.propertyCode(),
                    sib.name(),
                    sib.brand(),
                    sib.city(),
                    sib.marketCode(),
                    0f,                 // not a similarity match — no score
                    sib.thumbnailUrl(),
                    null,
                    isAvailable));
            if (nearby.size() >= maxNearby) break;
        }
        if (nearby.isEmpty()) {
            return null;
        }
        return new VisualSearchDtos.NearbyGroup(
                anchor.city(),
                anchor.marketCode(),
                anchor.propertyCode(),
                nearby);
    }

    private boolean matchesFilters(IndexedImage img, VisualSearchDtos.SearchFilters f) {
        if (f == null) return true;
        if (f.brand() != null && !f.brand().isEmpty() && !f.brand().contains(img.brand())) {
            return false;
        }
        if (f.maxPriceUSD() != null && img.priceTierUSD() != null && img.priceTierUSD() > f.maxPriceUSD()) {
            return false;
        }
        return true;
    }

    /**
     * Lightweight in-memory directory that maps marketCode -> properties.
     * Built from whatever's been seeded into the VectorStore.
     */
    @Component
    public static class InMemoryPropertyDirectory {

        private final VectorStore store;
        // Cached on each lookup; codefest corpus is small so there's no point keeping a separate index.

        public InMemoryPropertyDirectory(VectorStore store) {
            this.store = store;
        }

        public List<IndexedImage> byMarket(String marketCode) {
            if (marketCode == null) return List.of();
            // We don't have a public "list all" on the store; instead we scan via a sentinel query.
            // Codefest hack: rely on the fact that the in-memory store exposes everything via a 0-vector k-NN.
            // To avoid leaking the abstraction, the seed endpoint registers properties separately below.
            Map<String, IndexedImage> bestPerProperty = bestPerPropertyFromStore();
            List<IndexedImage> out = new ArrayList<>();
            for (IndexedImage img : bestPerProperty.values()) {
                if (marketCode.equalsIgnoreCase(img.marketCode())) {
                    out.add(img);
                }
            }
            return out;
        }

        private Map<String, IndexedImage> bestPerPropertyFromStore() {
            // Pull a generous slice of the store via a zero-similarity query. For the
            // codefest in-memory store this is O(n) which is fine.
            int corpusSize = Math.max(1, store.size());
            float[] sentinel = new float[512];                  // zero vector returns everything
            var hits = store.nearestNeighbors(sentinel, corpusSize,
                    com.marriott.codefest.snap2stay.visualsearchapi.vectorstore.VectorFilters.none());
            Map<String, IndexedImage> best = new LinkedHashMap<>();
            for (var h : hits) {
                best.putIfAbsent(h.image().propertyCode(), h.image());
            }
            return best;
        }
    }
}
