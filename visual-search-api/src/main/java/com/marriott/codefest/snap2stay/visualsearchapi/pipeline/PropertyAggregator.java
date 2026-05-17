package com.marriott.codefest.snap2stay.visualsearchapi.pipeline;

import com.marriott.codefest.snap2stay.visualsearchapi.domain.ScoredImage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multiple images can match the same property. Group by propertyCode and keep
 * the best-scoring image per property. Stable iteration order (LinkedHashMap)
 * means duplicates collapse to the highest-ranked image's slot.
 */
@Component
public class PropertyAggregator {

    public List<ScoredImage> aggregate(List<ScoredImage> imageHits) {
        Map<String, ScoredImage> bestByProperty = new LinkedHashMap<>();
        for (ScoredImage hit : imageHits) {
            String key = hit.image().propertyCode();
            ScoredImage existing = bestByProperty.get(key);
            if (existing == null || hit.combinedScore() > existing.combinedScore()) {
                bestByProperty.put(key, hit);
            }
        }
        return List.copyOf(bestByProperty.values());
    }
}
