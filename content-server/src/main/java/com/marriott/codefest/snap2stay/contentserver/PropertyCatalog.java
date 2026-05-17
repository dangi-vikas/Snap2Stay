package com.marriott.codefest.snap2stay.contentserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Loads seed property metadata from classpath JSON at startup. Image bytes are
 * resolved lazily from {@code seed-images/} on disk (or classpath as fallback).
 */
@Service
public class PropertyCatalog {

    private static final Logger log = LoggerFactory.getLogger(PropertyCatalog.class);

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${snap2stay.contentserver.seedFile:classpath:properties.json}")
    private String seedFile;

    @Value("${snap2stay.contentserver.imagesDir:seed-images}")
    private String imagesDir;

    private final Map<String, PropertyRecord> propertiesByCode = new ConcurrentHashMap<>();
    private final Map<String, String> imageIdToFile = new ConcurrentHashMap<>();

    public PropertyCatalog(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void load() throws IOException {
        Resource resource = resourceLoader.getResource(seedFile);
        if (!resource.exists()) {
            log.warn("Seed file {} not found — content server will return empty results", seedFile);
            return;
        }
        try (var in = resource.getInputStream()) {
            List<SeedEntry> entries = objectMapper.readValue(in, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SeedEntry.class));
            for (SeedEntry entry : entries) {
                PropertyRecord record = entry.toRecord();
                propertiesByCode.put(record.propertyCode(), record);
                for (Map.Entry<String, String> img : entry.images.entrySet()) {
                    imageIdToFile.put(img.getKey(), img.getValue());
                }
            }
            log.info("Loaded {} properties / {} images from {}",
                    propertiesByCode.size(), imageIdToFile.size(), seedFile);
        }
    }

    public List<PropertyRecord> listSince(Optional<Instant> since) {
        return propertiesByCode.values().stream()
                .filter(p -> since.isEmpty() || (p.lastModified() != null && p.lastModified().isAfter(since.get())))
                .sorted((a, b) -> a.propertyCode().compareTo(b.propertyCode()))
                .collect(Collectors.toList());
    }

    public Optional<byte[]> fetchImage(String imageId) throws IOException {
        String filename = imageIdToFile.get(imageId);
        if (filename == null) {
            return Optional.empty();
        }
        // Try filesystem first (so users can drop images without rebuilding)
        Path fsPath = Paths.get(imagesDir, filename);
        if (Files.exists(fsPath)) {
            return Optional.of(Files.readAllBytes(fsPath));
        }
        // Fallback: classpath
        Resource cpResource = resourceLoader.getResource("classpath:seed-images/" + filename);
        if (cpResource.exists()) {
            try (var in = cpResource.getInputStream()) {
                return Optional.of(in.readAllBytes());
            }
        }
        return Optional.empty();
    }

    public String resolveContentType(String imageId) {
        String filename = imageIdToFile.getOrDefault(imageId, "").toLowerCase();
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    /** Internal seed-file shape. {@code images} maps imageId -> filename relative to {@code imagesDir}. */
    public record SeedEntry(
            String propertyCode,
            String name,
            String brand,
            String city,
            String marketCode,
            double lat,
            double lon,
            Integer priceTierUSD,
            List<String> tags,
            Map<String, String> images,
            Instant lastModified
    ) {
        public PropertyRecord toRecord() {
            return new PropertyRecord(
                    propertyCode, name, brand, city, marketCode, lat, lon,
                    priceTierUSD,
                    tags == null ? List.of() : tags,
                    images == null ? List.of() : images.keySet().stream().sorted().toList(),
                    lastModified
            );
        }
    }
}
