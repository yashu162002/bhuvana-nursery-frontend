package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.storage.ImageStorage;
import com.bhuvana.nursery.storage.ImageStorageException;
import com.bhuvana.nursery.storage.StoredImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Admin image upload endpoint. The public API is unchanged; the bytes are
 * handed to the configured {@link ImageStorage} so production can persist them
 * in Cloudflare R2 instead of the ephemeral Render filesystem.
 */
@RestController
@RequestMapping("/api/v1/admin/uploads")
public class AdminUploadController {

    private static final Logger log = LoggerFactory.getLogger(AdminUploadController.class);

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 6_000;
    private static final long MAX_PIXELS = 24_000_000L;

    private final ImageStorage imageStorage;

    public AdminUploadController(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose a non-empty image smaller than 5 MB."
            );
        }

        byte[] content = file.getBytes();
        ImageFormat format = inspectImage(content);

        StoredImage stored = imageStorage.store(
                content, format.contentType(), format.extension());

        return Map.of(
                "url", stored.url(),
                "filename", stored.filename()
        );
    }

    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<Map<String, String>> handleStorageFailure(
            ImageStorageException exception
    ) {
        log.warn("Image upload failed (storage backend error: {}).",
                exception.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "message",
                        "Image storage is temporarily unavailable. Please try again."
                ));
    }

    /**
     * Validates the real image format instead of trusting the browser-supplied
     * MIME type or filename. SVG (scriptable) and every non-JPEG/PNG format
     * are rejected, and huge decompression bombs are refused before decoding.
     */
    private ImageFormat inspectImage(byte[] content) {
        try (ImageInputStream input =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {

            if (input == null) {
                throw invalidImage("The uploaded file is not a valid supported image.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw invalidImage("The uploaded file is not a valid supported image.");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0
                        || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw invalidImage(
                            "Image dimensions are too large (maximum 6000 px and 24 megapixels)."
                    );
                }

                if (reader.read(0) == null) {
                    throw invalidImage("The uploaded file is not a valid supported image.");
                }

                return formatOf(reader.getFormatName());
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw invalidImage("The uploaded file is not a valid supported image.");
        }
    }

    private ImageFormat formatOf(String formatName) {
        String normalized = formatName == null
                ? ""
                : formatName.toLowerCase(Locale.ROOT);

        if ("png".equals(normalized)) {
            return new ImageFormat("image/png", ".png");
        }

        if ("jpeg".equals(normalized) || "jpg".equals(normalized)) {
            return new ImageFormat("image/jpeg", ".jpg");
        }

        throw invalidImage("Only JPEG and PNG images are accepted.");
    }

    private ResponseStatusException invalidImage(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record ImageFormat(String contentType, String extension) {
    }
}
