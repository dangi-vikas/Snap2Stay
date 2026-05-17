# seed-images

Drop JPEG/PNG images here matching the filenames in `../src/main/resources/properties.json`.

The file scanner looks here first (filesystem) before falling back to the classpath. That means you can swap images without rebuilding — just restart the server.

## Expected files

Reference the `properties.json` for the canonical list. As of seed v1:

```
mlewh_01.jpg, mlewh_02.jpg, mlewh_03.jpg     # W Maldives
mleak_01.jpg, mleak_02.jpg                   # JW Marriott Maldives
mlerz_01.jpg, mlerz_02.jpg                   # Ritz-Carlton Maldives
nyceb_01.jpg, nyceb_02.jpg                   # St. Regis New York
nycmq_01.jpg, nycmq_02.jpg                   # Times Square EDITION
aseab_01.jpg, aseab_02.jpg                   # St. Regis Aspen
rakmk_01.jpg                                 # Marrakech Marriott
dxbjw_01.jpg, dxbjw_02.jpg                   # JW Marriott Marquis Dubai
```

## Where to source for the demo

- Marriott brand websites (their own marketing photos are usable for an internal codefest)
- Unsplash / Pexels with appropriate license, if substituting
- **Do not** ship copyrighted images outside the demo box

If a referenced file is missing, `GET /content/images/{imageId}` returns 404 and the ingestion service skips that image — non-fatal.
