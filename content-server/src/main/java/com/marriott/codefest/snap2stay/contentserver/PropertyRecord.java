package com.marriott.codefest.snap2stay.contentserver;

import java.time.Instant;
import java.util.List;

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
