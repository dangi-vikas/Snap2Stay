# visual-search-api

Public-facing query service. Spring Boot WebFlux, reactive end-to-end.

## Pipeline

1. **Image Preprocessor** — validate, resize, normalize, **strip EXIF unconditionally**. If `useImageLocation=true`, GPS is extracted into a transient `queryHints.geo` *before* stripping; never logged or persisted.
2. **Embedding Service client** — `POST /embed-and-tag` returns vector + auto-caption + auto-tags.
3. **Hybrid retrieval** — `0.7 · cosine(visual) + 0.3 · BM25(caption + tags)` against the vector store. Pre-filters: brand, region, opt-in geo radius.
4. **Property Aggregator** — group image hits by `propertyCode`, best score wins.
5. **Availability Filter** — stub in codefest; real AMP/ASP in prod.
6. **Re-ranker** — weighted score on (visual + personalization + price). Phase 1 rules-based.
7. **Location Expander** — for the top-1 match, fetch siblings in the same `marketCode`, re-apply filters, return as `nearbyInLocation`.
8. **Response Assembler** — `{ primaryMatches, nearbyInLocation }`.

## Endpoints

See [`../openapi/visual-search-api.yaml`](../openapi/visual-search-api.yaml).

- `POST /v1/visual-search` — public query
- `POST /v1/internal/seed` — internal seed endpoint used by image-ingestion-service (codefest only)
- `GET /v1/health/{live,ready}` — probes

## Run

```bash
mvn -pl visual-search-api -am spring-boot:run
# serves on http://localhost:8081
```

## In-memory vector store

The codefest impl is a `ConcurrentHashMap<String, IndexedImage>` with brute-force cosine similarity. Fine for ~10K images on a laptop. The `VectorStore` interface is the swap point for real OpenSearch in prod — the rest of the pipeline doesn't change.

## Privacy invariants (verify in tests)

- Image bytes never written to disk
- Image bytes never logged
- EXIF metadata always stripped before embedding
- `queryHints.geo` only present when `useImageLocation=true`, never appears in any log line, never returned in the response
