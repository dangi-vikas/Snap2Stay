package com.marriott.codefest.snap2stay.visualsearchapi.domain;

/**
 * Internal scored hit from the vector store. The combined score blends visual
 * cosine and lexical BM25 (over caption + tags); see HybridSearch for the math.
 */
public record ScoredImage(
        IndexedImage image,
        float visualScore,   // cosine similarity, normalized to [0,1]
        float textScore,     // BM25, normalized to [0,1]
        float combinedScore  // weighted blend
) implements Comparable<ScoredImage> {

    @Override
    public int compareTo(ScoredImage other) {
        return Float.compare(other.combinedScore, this.combinedScore);
    }
}
