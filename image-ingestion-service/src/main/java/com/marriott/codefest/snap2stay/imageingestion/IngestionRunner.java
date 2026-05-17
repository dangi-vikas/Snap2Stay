package com.marriott.codefest.snap2stay.imageingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One-shot batch ingestion runner.
 *
 * <p>Codefest behavior: on startup, list all properties from the content source,
 * vectorize every image, and POST the seed records to the visual-search-api in
 * batches. Logs progress, then exits successfully (or stays up — Spring Boot's
 * default keeps the JVM alive; in compose we let it idle).
 *
 * <p>Prod behavior (illustrative): replace {@link ApplicationRunner} with a
 * scheduled trigger or a Kafka consumer; replace {@link VectorStoreSeedClient}
 * with a direct OpenSearch bulk indexer.
 */
@Component
public class IngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionRunner.class);

    private final ContentSourceClient contentSource;
    private final EmbeddingServiceClient embedding;
    private final VectorStoreSeedClient vectorStore;
    private final int batchSize;
    private final int concurrency;

    public IngestionRunner(ContentSourceClient contentSource,
                           EmbeddingServiceClient embedding,
                           VectorStoreSeedClient vectorStore,
                           @Value("${snap2stay.ingestion.batchSize:8}") int batchSize,
                           @Value("${snap2stay.ingestion.concurrency:4}") int concurrency) {
        this.contentSource = contentSource;
        this.embedding = embedding;
        this.vectorStore = vectorStore;
        this.batchSize = batchSize;
        this.concurrency = concurrency;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting ingestion (batchSize={}, concurrency={})", batchSize, concurrency);

        long start = System.currentTimeMillis();
        Set<String> failedImages = new HashSet<>();

        Integer totalSeeded = contentSource.listAll()
                .doOnNext(props -> log.info("Got {} properties from content source", props.size()))
                .flatMapMany(this::expandToImageJobs)
                .flatMap(job -> embedOne(job).onErrorResume(err -> {
                    log.warn("Embed failed for image {}: {}", job.imageId, err.toString());
                    failedImages.add(job.imageId);
                    return Mono.empty();
                }), concurrency)
                .buffer(batchSize)
                .flatMap(batch -> vectorStore.seed(batch).map(VectorStoreSeedClient.SeedResponse::seeded))
                .reduce(0, Integer::sum)
                .blockOptional()
                .orElse(0);

        long ms = System.currentTimeMillis() - start;
        log.info("Ingestion done in {} ms — seeded={} failed={}", ms, totalSeeded, failedImages.size());
        if (!failedImages.isEmpty()) {
            log.warn("Failed image ids: {}", failedImages);
        }
    }

    private Flux<ImageJob> expandToImageJobs(List<PropertyRecord> props) {
        List<ImageJob> jobs = new ArrayList<>();
        for (PropertyRecord p : props) {
            for (String imageId : p.imageIds()) {
                jobs.add(new ImageJob(p, imageId));
            }
        }
        return Flux.fromIterable(jobs);
    }

    private Mono<VectorStoreSeedClient.SeedRequest> embedOne(ImageJob job) {
        return contentSource.fetchImage(job.imageId)
                .flatMap(bytes -> embedding.embedAndTag(bytes, job.imageId + ".jpg"))
                .map(emb -> new VectorStoreSeedClient.SeedRequest(
                        job.imageId,
                        job.property.propertyCode(),
                        job.property.name(),
                        job.property.brand(),
                        job.property.city(),
                        job.property.marketCode(),
                        job.property.lat(),
                        job.property.lon(),
                        job.property.priceTierUSD(),
                        emb.tags(),
                        emb.caption(),
                        emb.vectorArray(),
                        contentSource.thumbnailUrl(job.imageId)));
    }

    private record ImageJob(PropertyRecord property, String imageId) {}
}
