package com.marriott.codefest.snap2stay.contentserver;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/content")
public class ContentController {

    private final PropertyCatalog catalog;

    public ContentController(PropertyCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping(value = "/properties", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PropertyRecord> listProperties(@RequestParam(required = false) Instant since) {
        return catalog.listSince(Optional.ofNullable(since));
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> fetchImage(@PathVariable String imageId) throws IOException {
        Optional<byte[]> bytes = catalog.fetchImage(imageId);
        return bytes
                .map(b -> ResponseEntity.ok()
                        .header("Content-Type", catalog.resolveContentType(imageId))
                        .header("Cache-Control", "public, max-age=3600")
                        .body(b))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
