# seed-images

Drop JPEG/PNG images here matching the filenames in `../src/main/resources/properties.json`.

The file scanner looks here first (filesystem) before falling back to the classpath. That means you can swap images without rebuilding — just restart the server.

## Expected files

Reference the `properties.json` for the canonical list. Current seed:

```
Bloomington.jpeg          # Courtyard Bloomington
Chandigarh.jpeg           # JW Marriott Chandigarh
Dubai.jpeg                # JW Marriott Marquis Dubai
Hilton Head.jpeg          # Marriott Hilton Head Resort & Spa
Marco Island.jpeg         # JW Marriott Marco Island Beach Resort
Nashville.jpeg            # JW Marriott Nashville
Phuket.jpeg               # JW Marriott Phuket Resort & Spa
Saigon.jpeg               # JW Marriott Hotel Saigon
Udaipur Marriott.jpeg     # Udaipur Marriott Resort & Spa
```

## Where to source for the demo

- Marriott brand websites (their own marketing photos are usable for an internal codefest)
- Unsplash / Pexels with appropriate license, if substituting
- **Do not** ship copyrighted images outside the demo box

If a referenced file is missing, `GET /content/images/{imageId}` returns 404 and the ingestion service skips that image — non-fatal.
