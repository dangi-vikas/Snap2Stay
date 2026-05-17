package com.marriott.codefest.snap2stay.imageingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/** Calls the embedding service to vectorize images during ingestion. */
@Component
public class EmbeddingServiceClient {

    private final WebClient client;

    public EmbeddingServiceClient(
            WebClient.Builder builder,
            @Value("${snap2stay.ingestion.embedding.baseUrl:http://localhost:8082}") String baseUrl) {
        this.client = builder
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    public Mono<EmbedAndTagResponse> embedAndTag(byte[] imageBytes, String filename) {
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
                .bodyToMono(EmbedAndTagResponse.class);
    }

    public record EmbedAndTagResponse(
            List<Float> vector,
            String caption,
            List<String> tags,
            String modelName,
            int vectorDim
    ) {
        public float[] vectorArray() {
            float[] out = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) out[i] = vector.get(i);
            return out;
        }
    }
}
