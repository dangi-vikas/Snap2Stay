package com.marriott.codefest.snap2stay.visualsearchapi.preprocess;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Privacy and resize invariants for ImagePreprocessor. These should never regress.
 */
class ImagePreprocessorTest {

    private final ImagePreprocessor preprocessor = new ImagePreprocessor();

    @Test
    void preprocessReturnsBytesWithNoExifGps() throws Exception {
        byte[] plainJpeg = solidJpeg(800, 600);

        ImagePreprocessor.PreprocessResult result = preprocessor.preprocess(plainJpeg, false);

        Metadata md = ImageMetadataReader.readMetadata(new ByteArrayInputStream(result.cleanBytes()));
        boolean anyGps = !md.getDirectoriesOfType(GpsDirectory.class).isEmpty()
                && md.getFirstDirectoryOfType(GpsDirectory.class).getGeoLocation() != null;
        assertThat(anyGps).as("Output bytes must never carry EXIF GPS").isFalse();
    }

    @Test
    void preprocessProducesNoGeoHintWhenNotOptedIn() {
        byte[] plainJpeg = solidJpeg(800, 600);

        ImagePreprocessor.PreprocessResult result = preprocessor.preprocess(plainJpeg, /*useImageLocation*/ false);

        assertThat(result.queryHints().geo()).isNull();
    }

    @Test
    void preprocessProducesNoGeoHintWhenOptedInButImageHasNoGps() {
        byte[] plainJpeg = solidJpeg(800, 600);

        ImagePreprocessor.PreprocessResult result = preprocessor.preprocess(plainJpeg, /*useImageLocation*/ true);

        assertThat(result.queryHints().geo())
                .as("Opt-in but no GPS in image -> still no geo hint")
                .isNull();
    }

    @Test
    void preprocessResizesAboveMaxDimension() throws Exception {
        byte[] big = solidJpeg(2400, 1600);

        ImagePreprocessor.PreprocessResult result = preprocessor.preprocess(big, false);

        BufferedImage out = ImageIO.read(new ByteArrayInputStream(result.cleanBytes()));
        assertThat(out).isNotNull();
        assertThat(Math.max(out.getWidth(), out.getHeight())).isLessThanOrEqualTo(1024);
    }

    @Test
    void preprocessRejectsEmptyImage() {
        assertThatThrownBy(() -> preprocessor.preprocess(new byte[0], false))
                .isInstanceOf(ImageRejectedException.class)
                .matches(e -> ((ImageRejectedException) e).code().equals("EMPTY_IMAGE"));
    }

    @Test
    void preprocessRejectsImageOver10MB() {
        byte[] huge = new byte[11 * 1024 * 1024];

        assertThatThrownBy(() -> preprocessor.preprocess(huge, false))
                .isInstanceOf(ImageRejectedException.class)
                .matches(e -> ((ImageRejectedException) e).code().equals("IMAGE_TOO_LARGE"));
    }

    @Test
    void preprocessRejectsNonImageBytes() {
        byte[] notAnImage = "this is plain text, not a JPEG".getBytes();

        assertThatThrownBy(() -> preprocessor.preprocess(notAnImage, false))
                .isInstanceOf(ImageRejectedException.class)
                .matches(e -> ((ImageRejectedException) e).code().equals("BAD_IMAGE"));
    }

    private static byte[] solidJpeg(int w, int h) {
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, w, h);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpeg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
