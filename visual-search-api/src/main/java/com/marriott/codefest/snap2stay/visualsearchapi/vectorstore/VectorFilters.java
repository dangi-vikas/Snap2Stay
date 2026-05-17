package com.marriott.codefest.snap2stay.visualsearchapi.vectorstore;

import com.marriott.codefest.snap2stay.visualsearchapi.domain.QueryHints;

import java.util.List;

/**
 * Pre-filters applied at the vector store level. Cheaper than post-filtering
 * 200 hits, and crucial when an opt-in geo radius significantly narrows the search.
 */
public record VectorFilters(
        List<String> brands,        // empty = no brand filter
        String marketCode,          // null = no market filter
        Integer maxPriceUSD,        // null = no price filter
        QueryHints.GeoHint geo      // null = no geo filter
) {
    public static VectorFilters none() {
        return new VectorFilters(List.of(), null, null, null);
    }
}
