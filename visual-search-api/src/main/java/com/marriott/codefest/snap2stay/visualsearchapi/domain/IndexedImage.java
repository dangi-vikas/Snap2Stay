package com.marriott.codefest.snap2stay.visualsearchapi.domain;

import java.time.Instant;
import java.util.List;

/**
 * One indexed property image (record stored in the vector store).
 * Mirrors the prod OpenSearch document shape so the in-memory swap is a no-op.
 */
public record IndexedImage(
        String imageId,
        String propertyCode,
        String name,
        String brand,
        String city,
        String marketCode,
        double lat,
        double lon,
        Integer priceTierUSD,
        List<String> tags,
        String caption,
        float[] vector,
        String thumbnailUrl,
        Instant indexedAt
) {}
