package com.marriott.codefest.snap2stay.visualsearchapi.pipeline;

import com.marriott.codefest.snap2stay.visualsearchapi.api.VisualSearchDtos;
import org.springframework.stereotype.Component;

/**
 * Codefest stub for AMP/ASP availability. Deterministic by propertyCode hash so
 * the demo behaves consistently across reloads. Returns ~80% available.
 *
 * <p>Prod replacement: a reactive WebClient against the AMP/ASP availability service.
 */
@Component
public class AvailabilityStub {

    public boolean isAvailable(String propertyCode, VisualSearchDtos.DateRange dates) {
        if (dates == null) {
            return true;  // no dates -> assume available
        }
        // Deterministic ~80% pass rate: hash propertyCode and accept if hash mod 5 != 0.
        int h = Math.abs(propertyCode.hashCode());
        return h % 5 != 0;
    }
}
