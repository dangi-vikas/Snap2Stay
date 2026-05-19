package com.marriott.codefest.snap2stay.visualsearchapi.api;

import com.marriott.codefest.snap2stay.visualsearchapi.pipeline.VisualSearchPipeline;
import com.marriott.codefest.snap2stay.visualsearchapi.preprocess.ImageRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/visual-search")
public class VisualSearchController {

    private static final Logger log = LoggerFactory.getLogger(VisualSearchController.class);

    private final VisualSearchPipeline pipeline;

    public VisualSearchController(VisualSearchPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<VisualSearchDtos.VisualSearchResponse> searchMultipart(
            @RequestPart("image") FilePart image,
            @RequestPart(value = "useImageLocation", required = false) String useImageLocation,
            @RequestPart(value = "destination", required = false) String destination,
            @RequestPart(value = "checkIn", required = false) String checkIn,
            @RequestPart(value = "checkOut", required = false) String checkOut,
            @RequestPart(value = "brand", required = false) String brand,
            @RequestPart(value = "maxPriceUSD", required = false) String maxPriceUSD,
            @RequestPart(value = "marketCode", required = false) String marketCode) {

        long start = System.currentTimeMillis();
        boolean optInLocation = "true".equalsIgnoreCase(useImageLocation);
        VisualSearchDtos.SearchFilters filters = new VisualSearchDtos.SearchFilters(
                brand == null ? List.of() : List.of(brand.split(",")),
                maxPriceUSD == null ? null : Integer.parseInt(maxPriceUSD),
                marketCode);
        VisualSearchDtos.DateRange dates = parseDates(checkIn, checkOut);

        // Read FilePart content into byte array reactively
        return DataBufferUtils.join(image.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .doOnNext(bytes -> log.info("visualSearch (multipart) bytes={} useImageLocation={} destination={}",
                        bytes.length, optInLocation, destination))
                .flatMap(bytes -> pipeline.search(bytes, optInLocation, filters, dates))
                .map(resp -> withTookMs(resp, start));
    }

    private VisualSearchDtos.DateRange parseDates(String checkIn, String checkOut) {
        if (checkIn == null || checkOut == null) return null;
        try {
            return new VisualSearchDtos.DateRange(LocalDate.parse(checkIn), LocalDate.parse(checkOut));
        } catch (Exception e) {
            return null;
        }
    }

    private static VisualSearchDtos.VisualSearchResponse withTookMs(VisualSearchDtos.VisualSearchResponse r, long startMs) {
        return new VisualSearchDtos.VisualSearchResponse(
                r.primaryMatches(), r.nearbyInLocation(), r.queryId(),
                System.currentTimeMillis() - startMs, r.debug());
    }

    @ExceptionHandler(ImageRejectedException.class)
    public ResponseEntity<VisualSearchDtos.ErrorResponse> handleRejected(ImageRejectedException ex) {
        HttpStatus status = switch (ex.code()) {
            case "IMAGE_TOO_LARGE" -> HttpStatus.PAYLOAD_TOO_LARGE;
            case "EMPTY_IMAGE", "BAD_IMAGE", "ENCODE_FAILURE" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(new VisualSearchDtos.ErrorResponse(
                ex.code(), ex.getMessage(), null, UUID.randomUUID().toString()));
    }
}
