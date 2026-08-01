package com.dating.owoke.media.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.dating.owoke.media.asset.domain.MediaVariantName;
import com.dating.owoke.media.processing.configuration.MediaProcessingProperties;
import com.dating.owoke.media.processing.exception.InvalidImageException;

class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor(
            new MediaProcessingProperties(12 * 1024 * 1024, 40_000_000, java.time.Duration.ofDays(30)));

    @Test
    void createsEverySanitizedJpegVariant() throws Exception {
        BufferedImage source = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
        var graphics = source.createGraphics();
        graphics.setColor(new Color(130, 40, 80));
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(source, "jpg", bytes);

        ProcessedImage result = processor.process(bytes.toByteArray());

        assertThat(result.detectedContentType()).isEqualTo("image/jpeg");
        assertThat(result.originalWidth()).isEqualTo(900);
        assertThat(result.originalHeight()).isEqualTo(600);
        assertThat(result.sourceSha256()).hasSize(64);
        assertThat(result.variants()).extracting(ProcessedVariant::name)
                .containsExactly(MediaVariantName.values());
        assertThat(result.variants()).allSatisfy(variant -> {
            assertThat(variant.contentType()).isEqualTo("image/jpeg");
            assertThat(variant.content()).isNotEmpty();
            assertThat(variant.width()).isEqualTo(variant.name().width());
            assertThat(variant.height()).isEqualTo(variant.name().height());
            assertThat(variant.sha256()).hasSize(64);
            BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(variant.content()));
            assertThat(decoded.getWidth()).isEqualTo(variant.width());
            assertThat(decoded.getHeight()).isEqualTo(variant.height());
        });
    }

    @Test
    void rejectsContentThatOnlyPretendsToBeAnImage() {
        assertThatThrownBy(() -> processor.process("not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("JPEG, PNG and WebP");
    }
}
