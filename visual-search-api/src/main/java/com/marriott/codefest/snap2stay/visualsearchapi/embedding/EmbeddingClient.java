package com.marriott.codefest.snap2stay.visualsearchapi.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactive client for the Embedding Service. Stateless; safe to share.
 *
 * <p>Failures here trigger the "feature unavailable, fall back to text search"
 * response — they never propagate as 5xx unless the visual-search-api itself is broken.
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final WebClient client;

    public EmbeddingClient(
            WebClient.Builder builder,
            @Value("${snap2stay.embedding.baseUrl:http://localhost:8082}") String baseUrl) {
        this.client = builder
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
        log.info("EmbeddingClient configured with baseUrl={}", baseUrl);
    }

    /** Image -> vector + caption + tags. Used by both query and indexing pipelines. */
    public Mono<EmbeddingDtos.EmbedAndTagResponse> embedAndTag(byte[] imageBytes, String filename) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("image", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "image.jpg";
            }
        }).contentType(MediaType.IMAGE_JPEG);

        return client.post()
                .uri("/embed-and-tag")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .bodyToMono(EmbeddingDtos.EmbedAndTagResponse.class);
    }

    /** Text -> vector in the same space as image embeddings. Used for hybrid query. */
    public Mono<EmbeddingDtos.EmbedResponse> embedText(String text) {
        return client.post()
                .uri("/embed-text")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new EmbeddingDtos.EmbedTextRequest(text))
                .retrieve()
                .bodyToMono(EmbeddingDtos.EmbedResponse.class);
    }
}
