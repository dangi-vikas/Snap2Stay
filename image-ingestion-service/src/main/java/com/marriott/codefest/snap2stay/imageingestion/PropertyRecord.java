package com.marriott.codefest.snap2stay.imageingestion;

import java.time.Instant;
import java.util.List;

/**
 * Mirror of the content-server's PropertyRecord, kept duplicated here on purpose.
 * In prod each service would consume a shared API client jar; for codefest we
 * hand-write the DTO to keep dependencies between services minimal.
 */
public record PropertyRecord(
        String propertyCode,
        String name,
        String brand,
        String city,
        String marketCode,
        double lat,
        double lon,
        Integer priceTierUSD,
        List<String> tags,
        List<String> imageIds,
        Instant lastModified
) {}
