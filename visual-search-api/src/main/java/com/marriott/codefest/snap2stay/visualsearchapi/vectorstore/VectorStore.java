package com.marriott.codefest.snap2stay.visualsearchapi.vectorstore;

import com.marriott.codefest.snap2stay.visualsearchapi.domain.IndexedImage;

import java.util.Collection;
import java.util.List;

/**
 * Storage + retrieval boundary for property image vectors.
 *
 * <p>Codefest impl is in-memory with brute-force cosine. Prod impl wraps
 * AWS OpenSearch k-NN (HNSW) — same interface, swap one bean. The rest of the
 * pipeline doesn't know which one it's talking to.
 */
public interface VectorStore {

    /** Upsert a single record. */
    void put(IndexedImage image);

    /** Bulk upsert. */
    default void putAll(Collection<IndexedImage> images) {
        images.forEach(this::put);
    }

    /**
     * Visual k-NN. Returns the top-k images by cosine similarity to {@code queryVector},
     * filtered by the given pre-filters. Pre-filters always run at the store level
     * because filtering after k-NN would lose recall.
     */
    List<ScoredHit> nearestNeighbors(float[] queryVector, int k, VectorFilters filters);

    /** Number of indexed images. */
    int size();

    /** 
     * Returns the vector dimension used by this store.
     * Returns a default (768 for SigLIP) if the store is empty.
     */
    int getVectorDimension();

    /** A k-NN result with the cosine score against the query. */
    record ScoredHit(IndexedImage image, float cosineScore) {}
}
