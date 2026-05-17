# image-ingestion-service

Spring Boot WebFlux. Pulls property images + metadata from a `ContentSource`, calls the Embedding Service for vectors + auto-captions + tags, then seeds the Visual Search API's vector store.

## Codefest behavior

Runs **once at startup** and exits successfully when seeding is complete. No scheduler, no DLQ, no watermark persistence — just enough to populate the in-memory vector store before the demo.

## Prod behavior (illustrative)

Same code, configured differently:

- Trigger: AWS Step Functions (backfill) or scheduled cron (nightly delta) or Kafka consumer (Phase 2 real-time)
- `ContentSource` impl: Marriott Catalog HTTP client
- `VectorStore` write target: AWS OpenSearch k-NN bulk index
- Watermark in DynamoDB; failures to DLQ; metrics to Dynatrace

## Run

```bash
mvn -pl image-ingestion-service -am spring-boot:run
# requires content-server, embedding-service, and visual-search-api running
```

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `snap2stay.ingestion.contentSource.baseUrl` | `http://localhost:8083` | content-server |
| `snap2stay.ingestion.embedding.baseUrl` | `http://localhost:8082` | embedding-service |
| `snap2stay.ingestion.vectorStore.baseUrl` | `http://localhost:8081` | visual-search-api seed endpoint |
| `snap2stay.ingestion.batchSize` | `8` | images per embed call |
