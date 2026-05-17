package com.marriott.codefest.snap2stay.visualsearchapi.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

/** Request DTOs for the visual search endpoint. */
public final class VisualSearchDtos {

    private VisualSearchDtos() {}

    public record DateRange(LocalDate checkIn, LocalDate checkOut) {}

    public record SearchFilters(
            List<String> brand,
            Integer maxPriceUSD,
            String marketCode
    ) {
        public static SearchFilters empty() {
            return new SearchFilters(List.of(), null, null);
        }
    }

    /** Used when the request body is JSON (imageUrl variant). */
    public record VisualSearchJsonRequest(
            String imageUrl,
            Boolean useImageLocation,
            DateRange dates,
            String destination,
            SearchFilters filters,
            String consumerId
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PropertyMatch(
            String propertyCode,
            String name,
            String brand,
            String city,
            String marketCode,
            float matchScore,
            String thumbnailUrl,
            String explanation,
            boolean available
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NearbyGroup(
            String city,
            String marketCode,
            String anchorPropertyCode,
            List<PropertyMatch> properties
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VisualSearchResponse(
            List<PropertyMatch> primaryMatches,
            NearbyGroup nearbyInLocation,
            String queryId,
            long tookMs
    ) {}

    public record ErrorResponse(
            String code,
            String message,
            String suggestion,
            String correlationId
    ) {}
}
