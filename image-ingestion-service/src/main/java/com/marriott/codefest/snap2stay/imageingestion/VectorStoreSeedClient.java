package com.marriott.codefest.snap2stay.imageingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/** Pushes seed records to the visual-search-api's internal seed endpoint. */
@Component
public class VectorStoreSeedClient {

    private final WebClient client;

    public VectorStoreSeedClient(
            WebClient.Builder builder,
            @Value("${snap2stay.ingestion.vectorStore.baseUrl:http://localhost:8081}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public Mono<SeedResponse> seed(List<SeedRequest> records) {
        return client.post()
                .uri("/v1/internal/seed")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(records)
                .retrieve()
                .bodyToMono(SeedResponse.class);
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
}
