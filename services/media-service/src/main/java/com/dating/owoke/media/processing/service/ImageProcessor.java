package com.dating.owoke.media.processing.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

import com.dating.owoke.media.asset.domain.MediaVariantName;
import com.dating.owoke.media.processing.configuration.MediaProcessingProperties;
import com.dating.owoke.media.processing.exception.InvalidImageException;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

@Component
public class ImageProcessor {

    private static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

    private final MediaProcessingProperties properties;

    public ImageProcessor(MediaProcessingProperties properties) {
        this.properties = properties;
    }

    public ProcessedImage process(byte[] source) {
        if (source.length < 1 || source.length > properties.maximumSourceBytes()) {
            throw new InvalidImageException("Image size is outside the allowed range");
        }
        String detectedContentType = ImageFormatDetector.detect(source);
        Dimensions dimensions = inspect(source);
        long pixels = Math.multiplyExact((long) dimensions.width(), dimensions.height());
        if (pixels > properties.maximumPixels()) {
            throw new InvalidImageException("Image dimensions are too large");
        }

        BufferedImage oriented = readOriented(source);
        BufferedImage rgb = onWhiteBackground(oriented);
        List<ProcessedVariant> variants = new ArrayList<>();
        for (MediaVariantName name : MediaVariantName.values()) {
            variants.add(resize(rgb, name));
        }
        return new ProcessedImage(
                detectedContentType,
                oriented.getWidth(),
                oriented.getHeight(),
                Sha256.digest(source),
                List.copyOf(variants));
    }

    private Dimensions inspect(byte[] source) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidImageException("Image cannot be decoded");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                if (reader.getNumImages(true) != 1) {
                    throw new InvalidImageException("Animated images are not supported");
                }
                return new Dimensions(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidImageException("Image metadata cannot be read", exception);
        }
    }

    private BufferedImage readOriented(byte[] source) {
        try {
            return Thumbnails.of(new ByteArrayInputStream(source))
                    .useExifOrientation(true)
                    .scale(1)
                    .asBufferedImage();
        } catch (IOException exception) {
            throw new InvalidImageException("Image cannot be decoded", exception);
        }
    }

    private BufferedImage onWhiteBackground(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    private ProcessedVariant resize(BufferedImage source, MediaVariantName name) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Thumbnails.of(source)
                    .size(name.width(), name.height())
                    .crop(Positions.CENTER)
                    .outputFormat("jpg")
                    .outputQuality(name.quality())
                    .toOutputStream(output);
            byte[] content = output.toByteArray();
            return new ProcessedVariant(
                    name,
                    content,
                    OUTPUT_CONTENT_TYPE,
                    name.width(),
                    name.height(),
                    Sha256.digest(content));
        } catch (IOException exception) {
            throw new InvalidImageException("Image variant cannot be created", exception);
        }
    }

    private record Dimensions(int width, int height) {
    }
}
