package com.bhuvana.nursery;

import com.bhuvana.nursery.entity.AdminUser;
import com.bhuvana.nursery.repository.AdminUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level admin CRUD coverage: authorization, category + plant lifecycle
 * (including public visibility rules), and hero-slide lifecycle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCrudIntegrationTest {

    private static final String PASSWORD = "AdminCrudPass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;
    private MockHttpSession session;
    private Cookie csrfCookie;

    @BeforeEach
    void setUp() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        username = "crudadmin-" + unique;

        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
        admin.setEmail(username + "@example.com");
        admin.setEnabled(true);
        admin.setPasswordChangedAt(Instant.now());
        adminUserRepository.save(admin);

        csrfCookie = fetchCsrfCookie();
        session = new MockHttpSession();

        mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin endpoints reject anonymous requests")
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/plants"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/categories")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameKn":"ಅ","nameEn":"A","slug":"a","published":true}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Category and plant create/update/publish/archive/delete")
    void categoryAndPlantLifecycle() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String categorySlug = "test-category-" + unique;
        String plantSlug = "test-plant-" + unique;

        MvcResult categoryResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameKn":"ಪರೀಕ್ಷಾ ವರ್ಗ","nameEn":"Test Category %s",
                                 "slug":"%s","descriptionKn":null,"descriptionEn":null,
                                 "published":true}
                                """.formatted(unique, categorySlug)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(categorySlug))
                .andReturn();

        String categoryId = com.jayway.jsonpath.JsonPath.read(
                categoryResult.getResponse().getContentAsString(), "$.id");

        MvcResult plantResult = mockMvc.perform(post("/api/v1/admin/plants")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plantPayload(categoryId, plantSlug, 99.50)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(true))
                .andReturn();

        String plantId = com.jayway.jsonpath.JsonPath.read(
                plantResult.getResponse().getContentAsString(), "$.id");

        // Published plant and published category are publicly visible.
        mockMvc.perform(get("/api/v1/plants/{slug}", plantSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(plantSlug));

        // Update through the admin API and confirm the public read changes.
        mockMvc.perform(put("/api/v1/admin/plants/{id}", plantId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plantPayload(categoryId, plantSlug, 120.00)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(120.00));

        // Unpublishing hides it from the public catalogue.
        mockMvc.perform(patch("/api/v1/admin/plants/{id}/publish", plantId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .param("value", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));

        mockMvc.perform(get("/api/v1/plants/{slug}", plantSlug))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/admin/plants/{id}/publish", plantId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .param("value", "true"))
                .andExpect(status().isOk());

        // An unpublished category also hides its plants.
        mockMvc.perform(patch("/api/v1/admin/categories/{id}/publish", categoryId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .param("value", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));

        mockMvc.perform(get("/api/v1/plants/{slug}", plantSlug))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/admin/categories/{id}/publish", categoryId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .param("value", "true"))
                .andExpect(status().isOk());

        // Archive, restore and permanent delete.
        mockMvc.perform(delete("/api/v1/admin/plants/{id}", plantId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/api/v1/plants/{slug}", plantSlug))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/admin/plants/{id}/restore", plantId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(delete("/api/v1/admin/plants/{id}/permanent", plantId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/plants/{id}", plantId)
                        .session(session))
                .andExpect(status().isNotFound());

        // Category edits are persisted (there is intentionally no delete
        // endpoint while plants may reference the category).
        mockMvc.perform(put("/api/v1/admin/categories/{id}", categoryId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameKn":"ಪರೀಕ್ಷಾ ವರ್ಗ 2","nameEn":"Updated Category %s",
                                 "slug":"%s","descriptionKn":"ವಿವರಣೆ","descriptionEn":"Description",
                                 "published":true}
                                """.formatted(unique, categorySlug)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameEn").value("Updated Category " + unique));
    }

    @Test
    @DisplayName("Hero slides can be created, updated, toggled and deleted")
    void heroSlideLifecycle() throws Exception {
        String imageUrl = "https://images.example.com/hero-" + UUID.randomUUID() + ".jpg";

        MvcResult created = mockMvc.perform(post("/api/v1/admin/hero-slides")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl":"%s","altText":"Test slide",
                                 "titleKn":"ಪರೀಕ್ಷೆ","titleEn":"Test slide",
                                 "displayOrder":99,"active":true}
                                """.formatted(imageUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(imageUrl))
                .andReturn();

        String slideId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");

        MvcResult publicSlides = mockMvc.perform(get("/api/v1/hero-slides"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(publicSlides.getResponse().getContentAsString())
                .as("an active slide must appear in the public hero feed")
                .contains(slideId);

        mockMvc.perform(put("/api/v1/admin/hero-slides/{id}", slideId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl":"%s","altText":"Updated slide",
                                 "titleKn":"ಹೊಸ","titleEn":"Updated slide",
                                 "displayOrder":99,"active":true}
                                """.formatted(imageUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleEn").value("Updated slide"));

        mockMvc.perform(patch("/api/v1/admin/hero-slides/{id}/toggle-active", slideId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/admin/hero-slides/{id}", slideId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());

        MvcResult afterDelete = mockMvc.perform(get("/api/v1/hero-slides"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(afterDelete.getResponse().getContentAsString())
                .doesNotContain(slideId);
    }

    private String plantPayload(String categoryId, String slug, double price) {
        return """
                {"categoryId":"%s","nameKn":"ಪರೀಕ್ಷಾ ಸಸ್ಯ","nameEn":"Test Plant",
                 "scientificName":"Testus plantus","descriptionKn":null,"descriptionEn":null,
                 "slug":"%s","price":%s,"enquiryOnly":false,
                 "availability":"AVAILABLE","published":true,"featured":false,
                 "imageUrl":null}
                """.formatted(categoryId, slug, price);
    }

    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("XSRF-TOKEN cookie must be issued").isNotNull();
        return cookie;
    }
}
