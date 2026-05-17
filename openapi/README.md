# Snap2Stay — API Contracts

Three OpenAPI specs, one per service boundary. The UI codes against `visual-search-api.yaml`. The other two are internal.

| File | Service | Port | Public? |
|---|---|---|---|
| `visual-search-api.yaml` | `visual-search-api` (Spring WebFlux) | 8081 | **Yes** — guest-facing (in prod, fronted by Akana) |
| `embedding-service.yaml` | `embedding-service` (Python FastAPI + CLIP) | 8082 | No — internal only |
| `content-server.yaml` | `content-server` (codefest stand-in for Marriott Image Catalog) | 8083 | No — internal only; replaced in prod |

## Who calls whom

```
ui  ──►  visual-search-api  ──┬──►  embedding-service
                              └──►  (in-memory vector store, internal)

image-ingestion  ──►  content-server  ──►  embedding-service  ──►  visual-search-api (vector store seed endpoint)
```

## Browse the specs

Quickest local viewer:

```bash
docker run -p 8090:8080 -v "$(pwd):/specs" \
  -e SWAGGER_JSON=/specs/visual-search-api.yaml \
  swaggerapi/swagger-ui
```

Then open `http://localhost:8090`.
