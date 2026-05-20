package com.marriott.codefest.snap2stay.visualsearchapi.vectorstore;

import com.marriott.codefest.snap2stay.visualsearchapi.domain.IndexedImage;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory VectorStore with brute-force cosine. Adequate for &lt; 100K images
 * which is well past what we need for the codefest demo.
 *
 * <p>Activated by default; override with {@code snap2stay.vectorstore.type=opensearch}
 * once a real OpenSearch impl exists.
 */
@Component
@ConditionalOnProperty(name = "snap2stay.vectorstore.type", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorStore.class);

    private final Map<String, IndexedImage> byId = new ConcurrentHashMap<>();

    @Override
    public void put(IndexedImage image) {
        byId.put(image.imageId(), image);
    }

    @Override
    public int size() {
        return byId.size();
    }

    @Override
    public int getVectorDimension() {
        // Return dimension from first stored image, or default to 768 (SigLIP) if empty
        return byId.values().stream()
                .findFirst()
                .map(img -> img.vector().length)
                .orElse(768);
    }

    @Override
    public List<ScoredHit> nearestNeighbors(float[] queryVector, int k, VectorFilters filters) {
        if (byId.isEmpty()) {
            return List.of();
        }
        // Min-heap of size k keyed by cosine score; we pop the lowest when full.
        PriorityQueue<ScoredHit> heap = new PriorityQueue<>(k, Comparator.comparingDouble(ScoredHit::cosineScore));
        int considered = 0;
        for (IndexedImage img : byId.values()) {
            if (!matchesFilters(img, filters)) {
                continue;
            }
            considered++;
            float score = cosine(queryVector, img.vector());
            if (heap.size() < k) {
                heap.offer(new ScoredHit(img, score));
            } else if (heap.peek() != null && heap.peek().cosineScore() < score) {
                heap.poll();
                heap.offer(new ScoredHit(img, score));
            }
        }
        log.debug("k-NN scanned {} candidates, returning {} hits", considered, heap.size());
        List<ScoredHit> sorted = new ArrayList<>(heap);
        sorted.sort((a, b) -> Float.compare(b.cosineScore(), a.cosineScore()));
        return sorted;
    }

    private boolean matchesFilters(IndexedImage img, VectorFilters f) {
        if (f.brands() != null && !f.brands().isEmpty() && !f.brands().contains(img.brand())) {
            return false;
        }
        if (f.marketCode() != null && !f.marketCode().equalsIgnoreCase(img.marketCode())) {
            return false;
        }
        if (f.maxPriceUSD() != null && img.priceTierUSD() != null && img.priceTierUSD() > f.maxPriceUSD()) {
            return false;
        }
        if (f.geo() != null && !withinRadius(img, f.geo())) {
            return false;
        }
        return true;
    }

    private static boolean withinRadius(IndexedImage img, QueryHints.GeoHint geo) {
        return haversineKm(img.lat(), img.lon(), geo.lat(), geo.lon()) <= geo.radiusKm();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Cosine similarity. Vectors should arrive L2-normalized (CLIP returns
     * normalized features), so this becomes a plain dot product. We still
     * defensively normalize in case a caller forgets.
     */
    static float cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimension mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        if (denom == 0) return 0f;
        return (float) (dot / denom);
    }
}
