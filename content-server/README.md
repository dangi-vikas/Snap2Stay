# content-server

Codefest stand-in for Marriott's Property Image Catalog. Serves seed property images and metadata from local disk.

In prod this slot is filled by Marriott's existing CDN (or a partner feed: Meta, Pinterest). The Image Ingestion Service depends on the `ContentSource` interface, not on this implementation.

## Endpoints

See [`../openapi/content-server.yaml`](../openapi/content-server.yaml).

- `GET /content/properties[?since=ISO_TS]` → list property metadata + image references
- `GET /content/images/{imageId}` → image bytes (image/jpeg or image/png)

## Run

```bash
mvn -pl content-server -am spring-boot:run
# serves on http://localhost:8083
```

## Seeding images

Drop JPEG/PNG files into `seed-images/`, then list them in `src/main/resources/properties.json` (see the seed file for the shape).

The file scanner is naive on purpose: change a file, restart the server.
