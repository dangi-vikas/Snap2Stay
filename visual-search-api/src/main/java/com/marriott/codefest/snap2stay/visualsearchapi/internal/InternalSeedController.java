package com.marriott.codefest.snap2stay.visualsearchapi.internal;

import com.marriott.codefest.snap2stay.visualsearchapi.domain.IndexedImage;
import com.marriott.codefest.snap2stay.visualsearchapi.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Internal endpoints used by the image-ingestion-service to seed the vector store.
 *
 * <p>NOT publicly exposed. In prod the ingestion writes directly to OpenSearch
 * and this endpoint doesn't exist; in codefest it lets us keep all state local
 * to one process so a restart of the API rebuilds easily.
 */
@RestController
@RequestMapping("/v1/internal")
public class InternalSeedController {

    private static final Logger log = LoggerFactory.getLogger(InternalSeedController.class);

    private final VectorStore vectorStore;

    public InternalSeedController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping("/seed")
    public ResponseEntity<SeedResponse> seed(@RequestBody List<SeedRequest> requests) {
        for (SeedRequest req : requests) {
            vectorStore.put(new IndexedImage(
                    req.imageId, req.propertyCode, req.name, req.brand, req.city,
                    req.marketCode, req.lat, req.lon, req.priceTierUSD,
                    req.tags, req.caption,
                    req.vector,
                    req.thumbnailUrl,
                    Instant.now()));
        }
        log.info("Seeded {} images; total in store: {}", requests.size(), vectorStore.size());
        return ResponseEntity.ok(new SeedResponse(requests.size(), vectorStore.size()));
    }

    @GetMapping("/seed/stats")
    public SeedStats stats() {
        return new SeedStats(vectorStore.size());
    }

    public record SeedRequest(
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
            String thumbnailUrl
    ) {}

    public record SeedResponse(int seeded, int totalInStore) {}
    public record SeedStats(int totalIndexed) {}
}
