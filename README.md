# Snap2Stay

> Codefest 4.0 — Marriott visual property search.
> Snap a photo, find the matching Marriott property, plus other places in that destination.

Architecture: `[../docs/VisualSearchArchitecture.md](../docs/VisualSearchArchitecture.md)`
Demo plan: `[../docs/CodefestPlan.md](../docs/CodefestPlan.md)`
Diagrams: `[../docs/diagrams/Snap2StayDiagrams.html](../docs/diagrams/Snap2StayDiagrams.html)`

## Layout

```
Snap2Stay/
├── ui/                          # frontend (separate effort)
├── content-server/              # Java — codefest stand-in for Marriott Image Catalog (port 8083)
├── image-ingestion-service/     # Java — pulls + embeds + seeds the vector store (no port; runs once)
├── visual-search-api/           # Java — public query API (port 8081)
├── embedding-service/           # Python FastAPI + CLIP (port 8082)
├── openapi/                     # OpenAPI specs for the three HTTP services
├── pom.xml                      # Maven aggregator for the three Java modules
└── docker-compose.yml           # one-command local stack (TODO)
```

## Stack

- **Java 21**, **Spring Boot 3.2.5** (matches Marriott TAP team conventions)
- **Spring WebFlux** for the API + ingestion (reactive, matches RDS-tier pattern)
- **Resilience4j** for client circuit breakers
- **metadata-extractor** (drewnoakes) for EXIF read/strip
- **Python 3.11** + **FastAPI** + **HuggingFace transformers** (CLIP) for embeddings

## Build

```bash
# All three Java services at once
mvn -s .mvn/settings-public.xml clean install

# One service at a time
mvn -s .mvn/settings-public.xml -pl visual-search-api -am clean install
```

> The `-s .mvn/settings-public.xml` flag forces dependency resolution through public Maven Central. Drop it inside Marriott VPN if you prefer Artifactory.

## Quick start (Docker Compose)

```bash
docker compose up --build
```

This brings up the four services in dependency order:

1. **embedding** (CLIP loads once; ~30 s cold start)
2. **content-server** (seed catalog of 8 Marriott properties)
3. **visual-search-api** (waits for embedding to be healthy)
4. **ingestion** (runs once, embeds all seed images, seeds the vector store, then sleeps)

The first build takes ~5 min (Python image bakes CLIP weights into the layer cache); subsequent runs are fast.

When all services report healthy, hit:

```bash
curl -X POST http://localhost:8081/v1/visual-search \
  -F "image=@/path/to/test.jpg"
```

## Run manually (no Docker)

Starting from a fresh terminal in `Snap2Stay/`:

```bash
# 1. Embedding service (slow first start — downloads CLIP weights once)
cd embedding-service && pip install -r requirements.txt && uvicorn main:app --port 8082

# 2. Content server
mvn -s .mvn/settings-public.xml -pl content-server spring-boot:run

# 3. Visual search API
mvn -s .mvn/settings-public.xml -pl visual-search-api spring-boot:run

# 4. Ingestion (runs once and exits)
mvn -s .mvn/settings-public.xml -pl image-ingestion-service spring-boot:run
```

Or use the docker-compose at the bottom of this folder once it's wired up.

## Sanity check

```bash
curl -X POST http://localhost:8081/v1/visual-search \
  -F "image=@/path/to/test.jpg" \
  -F "useImageLocation=false"
```

You should get a JSON response with `primaryMatches[]` and `nearbyInLocation`.