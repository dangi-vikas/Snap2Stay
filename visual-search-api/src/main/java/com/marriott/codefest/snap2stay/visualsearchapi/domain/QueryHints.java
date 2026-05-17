package com.marriott.codefest.snap2stay.visualsearchapi.domain;

/**
 * Transient query hints derived from the request. Held in memory only,
 * never logged, never persisted. Populated when {@code useImageLocation=true}
 * and the uploaded image carries EXIF GPS.
 *
 * @param geo opt-in geo extracted from EXIF; null when not opted-in or no GPS
 */
public record QueryHints(GeoHint geo) {

    public static QueryHints empty() {
        return new QueryHints(null);
    }

    public record GeoHint(double lat, double lon, double radiusKm) {}
}
