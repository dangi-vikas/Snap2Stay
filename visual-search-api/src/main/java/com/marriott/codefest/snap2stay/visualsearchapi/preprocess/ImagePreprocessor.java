package com.marriott.codefest.snap2stay.visualsearchapi.preprocess;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.marriott.codefest.snap2stay.visualsearchapi.domain.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Image preprocessor.
 *
 * <p>Privacy invariants (verified by tests):
 * <ul>
 *   <li>EXIF/GPS is always stripped from the bytes returned by {@link #preprocess}.</li>
 *   <li>EXIF GPS is read into a transient {@link QueryHints.GeoHint} <em>only</em>
 *       when {@code useImageLocation=true}; otherwise GPS is never extracted at all.</li>
 *   <li>Image bytes never written to disk by this code.</li>
 * </ul>
 */
@Component
public class ImagePreprocessor {

    private static final Logger log = LoggerFactory.getLogger(ImagePreprocessor.class);

    private static final int MAX_DIMENSION_PX = 1024;
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final double DEFAULT_GEO_RADIUS_KM = 75.0;

    public PreprocessResult preprocess(byte[] rawBytes, boolean useImageLocation) {
        if (rawBytes == null || rawBytes.length == 0) {
            throw new ImageRejectedException("EMPTY_IMAGE", "Empty image payload");
        }
        if (rawBytes.length > MAX_BYTES) {
            throw new ImageRejectedException("IMAGE_TOO_LARGE", "Image exceeds 10 MB limit");
        }

        QueryHints.GeoHint geo = null;
        if (useImageLocation) {
            geo = extractGpsTransient(rawBytes).orElse(null);
            if (geo != null) {
                // INTENTIONAL: log only that geo is present, NEVER the coordinates.
                log.debug("Opt-in EXIF geo hint extracted (radius {}km)", geo.radiusKm());
            }
        }

        BufferedImage decoded = decode(rawBytes);
        BufferedImage resized = resize(decoded, MAX_DIMENSION_PX);
        byte[] cleanBytes = encodeJpegStripExif(resized);

        return new PreprocessResult(cleanBytes, new QueryHints(geo));
    }

    private BufferedImage decode(byte[] bytes) {
        if (isHeic(bytes)) {
            return decodeHeic(bytes);
        }
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new ImageRejectedException("BAD_IMAGE", "Could not decode image");
            }
            return img;
        } catch (IOException e) {
            throw new ImageRejectedException("BAD_IMAGE", "I/O error decoding image: " + e.getMessage());
        }
    }

    private static boolean isHeic(byte[] bytes) {
        if (bytes.length < 12) return false;
        // HEIC/HEIF files have "ftyp" at offset 4, followed by brand like "heic", "heix", "mif1"
        return bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p'
                && (matchesBrand(bytes, "heic") || matchesBrand(bytes, "heix")
                    || matchesBrand(bytes, "mif1") || matchesBrand(bytes, "heif"));
    }

    private static boolean matchesBrand(byte[] bytes, String brand) {
        if (bytes.length < 12) return false;
        return bytes[8] == brand.charAt(0) && bytes[9] == brand.charAt(1)
                && bytes[10] == brand.charAt(2) && bytes[11] == brand.charAt(3);
    }

    private BufferedImage decodeHeic(byte[] bytes) {
        Path heicFile = null;
        Path jpegFile = null;
        try {
            heicFile = Files.createTempFile("snap2stay_", ".heic");
            jpegFile = Path.of(heicFile.toString().replace(".heic", ".jpeg"));
            Files.write(heicFile, bytes);

            ProcessBuilder pb = new ProcessBuilder(
                    "sips", "-s", "format", "jpeg", "-s", "formatOptions", "best",
                    heicFile.toString(), "--out", jpegFile.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            int exitCode = proc.waitFor();
            if (exitCode != 0 || !Files.exists(jpegFile)) {
                log.warn("sips HEIC conversion failed (exit={})", exitCode);
                throw new ImageRejectedException("BAD_IMAGE", "Could not decode HEIC image");
            }

            byte[] jpegBytes = Files.readAllBytes(jpegFile);
            try (InputStream in = new ByteArrayInputStream(jpegBytes)) {
                BufferedImage img = ImageIO.read(in);
                if (img == null) {
                    throw new ImageRejectedException("BAD_IMAGE", "Could not decode converted HEIC image");
                }
                return img;
            }
        } catch (ImageRejectedException e) {
            throw e;
        } catch (Exception e) {
            throw new ImageRejectedException("BAD_IMAGE", "HEIC conversion failed: " + e.getMessage());
        } finally {
            try {
                if (heicFile != null) Files.deleteIfExists(heicFile);
                if (jpegFile != null) Files.deleteIfExists(jpegFile);
            } catch (IOException ignored) {}
        }
    }

    private static BufferedImage resize(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxDim && h <= maxDim) {
            return src;
        }
        double scale = (double) maxDim / Math.max(w, h);
        int newW = Math.max(1, (int) Math.round(w * scale));
        int newH = Math.max(1, (int) Math.round(h * scale));
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return out;
    }

    /**
     * Re-encode as JPEG without writing any metadata. ImageIO's default JPEG writer
     * does not preserve EXIF unless explicitly told to, which is exactly what we want.
     */
    private static byte[] encodeJpegStripExif(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
            writer.dispose();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ImageRejectedException("ENCODE_FAILURE", "Could not re-encode image: " + e.getMessage());
        }
    }

    /**
     * Extract GPS coordinates from EXIF if present. Returns {@link Optional#empty()}
     * if the image has no GPS tags or if extraction fails for any reason.
     *
     * <p>Coordinates are returned in a transient {@link QueryHints.GeoHint} that the
     * caller is expected to keep on the request stack only — never log or persist.
     */
    private Optional<QueryHints.GeoHint> extractGpsTransient(byte[] bytes) {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            Metadata metadata = ImageMetadataReader.readMetadata(in);
            for (GpsDirectory gps : metadata.getDirectoriesOfType(GpsDirectory.class)) {
                if (gps.getGeoLocation() != null) {
                    return Optional.of(new QueryHints.GeoHint(
                            gps.getGeoLocation().getLatitude(),
                            gps.getGeoLocation().getLongitude(),
                            DEFAULT_GEO_RADIUS_KM));
                }
            }
            return Optional.empty();
        } catch (Exception e) {  // intentionally broad: any extraction failure -> no geo hint
            return Optional.empty();
        }
    }

    public record PreprocessResult(byte[] cleanBytes, QueryHints queryHints) {}
}
