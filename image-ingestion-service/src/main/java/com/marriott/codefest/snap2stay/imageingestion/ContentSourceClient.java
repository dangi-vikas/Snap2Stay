package com.marriott.codefest.snap2stay.imageingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reads the catalog from the content-server. In prod this slot is filled by
 * the Marriott Property Image Catalog client (or a partner-feed fetcher).
 */
@Component
public class ContentSourceClient {

    private final WebClient client;
    private final String baseUrl;

    public ContentSourceClient(
            WebClient.Builder builder,
            @Value("${snap2stay.ingestion.contentSource.baseUrl:http://localhost:8083}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = builder.baseUrl(baseUrl).build();
    }

    public Mono<List<PropertyRecord>> listAll() {
        return client.get()
                .uri("/content/properties")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PropertyRecord>>() {});
    }

    public Mono<byte[]> fetchImage(String imageId) {
        return client.get()
                .uri("/content/images/{id}", imageId)
                .retrieve()
                .bodyToMono(byte[].class);
    }

    /** Browser-reachable URL for an image, used as the thumbnail for matches. */
    public String thumbnailUrl(String imageId) {
        return baseUrl + "/content/images/" + imageId;
    }
}
