package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.storage.ImageStorage;
import com.bhuvana.nursery.storage.ImageStorageException;
import com.bhuvana.nursery.storage.StoredImage;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level coverage of the upload endpoint after the storage abstraction was
 * introduced: authentication, CSRF, real image validation and the generic
 * storage-failure response.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorage imageStorage;

    @Test
    void uploadRequiresAuthentication() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(pngFile())
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(imageStorage);
    }

    @Test
    void uploadRequiresACsrfToken() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(pngFile())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(imageStorage);
    }

    @Test
    void rejectsBytesThatAreNotAJpegOrPng() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();
        MockMultipartFile fake = new MockMultipartFile(
                "file",
                "plant.png",
                MediaType.IMAGE_PNG_VALUE,
                "not really an image".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(fake)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(imageStorage);
    }

    @Test
    void storesAValidPngThroughTheConfiguredStorage() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();
        when(imageStorage.store(any(byte[].class), eq("image/png"), eq(".png")))
                .thenReturn(new StoredImage(
                        "https://images.example.com/uploads/generated.png",
                        "generated.png"
                ));

        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(pngFile())
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url")
                        .value("https://images.example.com/uploads/generated.png"))
                .andExpect(jsonPath("$.filename").value("generated.png"));

        verify(imageStorage).store(any(byte[].class), eq("image/png"), eq(".png"));
    }

    @Test
    void storageFailuresReturn503WithoutLeakingDetails() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();
        when(imageStorage.store(any(byte[].class), any(), any()))
                .thenThrow(new ImageStorageException(
                        "Could not upload: secret-internal-detail",
                        new RuntimeException("bucket token")
                ));

        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(pngFile())
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Image storage is temporarily unavailable. Please try again."));
    }

    private MockMultipartFile pngFile() throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        return new MockMultipartFile(
                "file",
                "plant.png",
                MediaType.IMAGE_PNG_VALUE,
                output.toByteArray()
        );
    }

    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        if (cookie == null) {
            throw new AssertionError("Expected an XSRF-TOKEN cookie");
        }
        return cookie;
    }
}
