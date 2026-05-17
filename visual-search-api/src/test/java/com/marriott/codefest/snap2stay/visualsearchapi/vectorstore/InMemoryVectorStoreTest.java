package com.marriott.codefest.snap2stay.visualsearchapi.vectorstore;

import com.marriott.codefest.snap2stay.visualsearchapi.domain.IndexedImage;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.QueryHints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVectorStoreTest {

    private InMemoryVectorStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryVectorStore();
    }

    @Test
    void cosineOfIdenticalVectorsIs1() {
        float[] a = {1f, 0f, 0f};
        assertThat(InMemoryVectorStore.cosine(a, a)).isCloseTo(1f, org.assertj.core.data.Offset.offset(1e-5f));
    }

    @Test
    void cosineOfOrthogonalVectorsIs0() {
        float[] a = {1f, 0f, 0f};
        float[] b = {0f, 1f, 0f};
        assertThat(InMemoryVectorStore.cosine(a, b)).isCloseTo(0f, org.assertj.core.data.Offset.offset(1e-5f));
    }

    @Test
    void nearestNeighborsRanksByCosine() {
        store.put(image("a", "P1", new float[]{1f, 0f, 0f}, "Maldives", "MLE"));
        store.put(image("b", "P2", new float[]{0.9f, 0.1f, 0f}, "Maldives", "MLE"));
        store.put(image("c", "P3", new float[]{0f, 1f, 0f}, "New York", "NYC"));

        var hits = store.nearestNeighbors(new float[]{1f, 0f, 0f}, 3, VectorFilters.none());

        assertThat(hits).hasSize(3);
        assertThat(hits.get(0).image().imageId()).isEqualTo("a");
        assertThat(hits.get(1).image().imageId()).isEqualTo("b");
        assertThat(hits.get(2).image().imageId()).isEqualTo("c");
    }

    @Test
    void nearestNeighborsHonorsBrandFilter() {
        store.put(imageWithBrand("a", "P1", "W Hotels", new float[]{1f, 0f, 0f}));
        store.put(imageWithBrand("b", "P2", "Ritz-Carlton", new float[]{0.99f, 0.01f, 0f}));

        var hits = store.nearestNeighbors(
                new float[]{1f, 0f, 0f}, 5,
                new VectorFilters(List.of("W Hotels"), null, null, null));

        assertThat(hits).extracting(s -> s.image().brand()).containsExactly("W Hotels");
    }

    @Test
    void nearestNeighborsHonorsGeoRadius() {
        // Maldives lat/lon
        store.put(imageAtLocation("a", "P1", 4.1755, 73.5093));
        // New York lat/lon — far away
        store.put(imageAtLocation("b", "P2", 40.7616, -73.9747));

        QueryHints.GeoHint nearMaldives = new QueryHints.GeoHint(4.5, 73.5, 200.0);  // 200km radius

        var hits = store.nearestNeighbors(
                new float[]{1f, 0f, 0f}, 5,
                new VectorFilters(List.of(), null, null, nearMaldives));

        assertThat(hits).extracting(s -> s.image().imageId()).containsExactly("a");
    }

    private static IndexedImage image(String id, String code, float[] vec, String city, String market) {
        return new IndexedImage(id, code, "Some Hotel", "Some Brand", city, market,
                0.0, 0.0, 500, List.of(), "", vec, "http://example/x.jpg", Instant.EPOCH);
    }

    private static IndexedImage imageWithBrand(String id, String code, String brand, float[] vec) {
        return new IndexedImage(id, code, "Some Hotel", brand, "Maldives", "MLE",
                4.1755, 73.5093, 500, List.of(), "", vec, "http://example/x.jpg", Instant.EPOCH);
    }

    private static IndexedImage imageAtLocation(String id, String code, double lat, double lon) {
        return new IndexedImage(id, code, "Hotel " + code, "Brand", "City", "MKT",
                lat, lon, 500, List.of(), "", new float[]{1f, 0f, 0f}, "http://example/x.jpg", Instant.EPOCH);
    }
}
