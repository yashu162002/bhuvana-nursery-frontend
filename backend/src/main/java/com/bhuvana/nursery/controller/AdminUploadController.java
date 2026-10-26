package com.bhuvana.nursery.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/uploads")
public class AdminUploadController {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png");

    private final Path uploadDirectory;

    public AdminUploadController(
            @Value("${app.upload.directory:./uploads}") String directory
    ) {
        this.uploadDirectory = Path.of(directory)
                .toAbsolutePath()
                .normalize();
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

        String contentType = file.getContentType();
        if (contentType == null ||
                !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPEG and PNG images are accepted."
            );
        }

        // Decode the image rather than trusting the filename or MIME type alone.
        try (var input = file.getInputStream()) {
            if (ImageIO.read(input) == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The uploaded file is not a valid supported image."
                );
            }
        }

        String extension = "image/png".equalsIgnoreCase(contentType)
                ? ".png"
                : ".jpg";

        Files.createDirectories(uploadDirectory);

        String filename = UUID.randomUUID() + extension;
        Path target = uploadDirectory.resolve(filename).normalize();

        if (!target.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid upload filename."
            );
        }

        // CREATE_NEW prevents accidentally overwriting an existing file.
        try (var input = file.getInputStream()) {
            Files.copy(input, target);
        }

        return Map.of(
                "url", "/uploads/" + filename,
                "filename", filename
        );
    }
}
