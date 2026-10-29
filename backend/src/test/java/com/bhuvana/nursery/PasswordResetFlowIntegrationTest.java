package com.bhuvana.nursery;

import com.bhuvana.nursery.entity.AdminUser;
import com.bhuvana.nursery.entity.PasswordResetToken;
import com.bhuvana.nursery.repository.AdminUserRepository;
import com.bhuvana.nursery.repository.PasswordResetTokenRepository;
import com.bhuvana.nursery.service.InMemoryRateLimiter;
import com.bhuvana.nursery.service.PasswordResetEmailSender;
import com.bhuvana.nursery.service.PasswordResetService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the reset flow over real HTTP endpoints and a real
 * PostgreSQL schema (Flyway), with a mocked email sender standing in for the
 * administrator's inbox.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetFlowIntegrationTest {

    private static final String ORIGINAL_PASSWORD = "OriginalPass123";
    private static final String NEW_PASSWORD = "FreshPass456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InMemoryRateLimiter rateLimiter;

    @MockitoBean
    private PasswordResetEmailSender emailSender;

    private String username;
    private String email;

    @BeforeEach
    void setUp() {
        rateLimiter.reset();
        when(emailSender.isConfigured()).thenReturn(true);

        // A fresh, self-contained administrator per test so runs stay
        // deterministic no matter what earlier runs left behind.
        String unique = UUID.randomUUID().toString().substring(0, 8);
        username = "resetadmin-" + unique;
        email = username + "@example.com";

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(username);
        adminUser.setPasswordHash(passwordEncoder.encode(ORIGINAL_PASSWORD));
        adminUser.setEmail(email);
        adminUser.setEnabled(true);
        adminUser.setPasswordChangedAt(Instant.now());
        adminUserRepository.save(adminUser);
    }

    // ---------------------------------------------------------------- login

    @Test
    @DisplayName("Existing username/password login still works against the database")
    void loginStillWorksForDatabaseBackedAdmin() throws Exception {
        MockHttpSession session = login(ORIGINAL_PASSWORD);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("After a reset the new password logs in and the old one does not")
    void loginReflectsResetPassword() throws Exception {
        String rawToken = requestResetLink();
        resetPassword(rawToken, NEW_PASSWORD);

        postWithCsrf("/api/v1/auth/login", loginJson(NEW_PASSWORD), new MockHttpSession())
                .andExpect(status().isOk());

        postWithCsrf("/api/v1/auth/login", loginJson(ORIGINAL_PASSWORD), new MockHttpSession())
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------- forgot

    @Test
    @DisplayName("A known email returns the generic message and sends one link")
    void forgotPasswordSendsLinkForKnownEmail() throws Exception {
        postWithCsrf("/api/v1/auth/password/forgot", forgotJson(email), null)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(emailSender, atLeastOnce())
                .sendResetLink(org.mockito.ArgumentMatchers.eq(email), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("An unknown email gets the identical generic message and no email")
    void forgotPasswordIsNotEnumerable() throws Exception {
        String unknownEmail = "definitely-not-registered-" + UUID.randomUUID() + "@example.com";

        postWithCsrf("/api/v1/auth/password/forgot", forgotJson(unknownEmail), null)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(emailSender, never()).sendResetLink(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("When SMTP is not configured the endpoint answers 503 and sends nothing")
    void forgotPasswordIsUnavailableWithoutSmtp() throws Exception {
        when(emailSender.isConfigured()).thenReturn(false);

        postWithCsrf("/api/v1/auth/password/forgot", forgotJson(email), null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "Password reset is temporarily unavailable. Please try again later."));

        verify(emailSender, never())
                .sendResetLink(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Missing or malformed email bodies are rejected with 400")
    void forgotPasswordValidatesInput() throws Exception {
        postWithCsrf("/api/v1/auth/password/forgot", "{\"email\":\"not-an-email\"}", null)
                .andExpect(status().isBadRequest());

        postWithCsrf("/api/v1/auth/password/forgot", "{}", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Repeated forgot requests for one address are limited")
    void forgotPasswordIsRateLimited() throws Exception {
        for (int i = 0; i < 3; i++) {
            postWithCsrf("/api/v1/auth/password/forgot", forgotJson(email), null)
                    .andExpect(status().isAccepted());
        }

        postWithCsrf("/api/v1/auth/password/forgot", forgotJson(email), null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // --------------------------------------------------------------- reset

    @Test
    @DisplayName("The emailed link resets the password and the change persists in the database")
    void fullResetFlowPersistsNewPassword() throws Exception {
        String rawToken = requestResetLink();

        // Only a hash is persisted - the raw token never lands in a column.
        String expectedHash = PasswordResetService.sha256Hex(rawToken);
        assertThat(tokenRepository.findAll())
                .anySatisfy(token -> assertThat(token.getTokenHash())
                        .isEqualTo(expectedHash)
                        .hasSize(64)
                        .isNotEqualTo(rawToken));

        resetPassword(rawToken, NEW_PASSWORD);

        AdminUser reloaded = adminUserRepository.findByUsername(username).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, reloaded.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(ORIGINAL_PASSWORD, reloaded.getPasswordHash())).isFalse();

        // The token is consumed, marked used, and can never be replayed.
        PasswordResetToken usedToken = tokenRepository.findByTokenHash(expectedHash).orElseThrow();
        assertThat(usedToken.isUsed()).isTrue();

        postWithCsrf("/api/v1/auth/password/reset", resetJson(rawToken, NEW_PASSWORD), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Requesting a new link invalidates the previous one")
    void newestLinkOnlyRemainsValid() throws Exception {
        String firstToken = requestResetLink();
        String secondToken = requestResetLink();

        assertThat(firstToken).isNotEqualTo(secondToken);

        // The first link was deleted when the second was issued.
        assertThat(tokenRepository.findByTokenHash(PasswordResetService.sha256Hex(firstToken)))
                .isEmpty();

        resetPassword(secondToken, NEW_PASSWORD);
    }

    @Test
    @DisplayName("An expired token is rejected and the password stays unchanged")
    void expiredTokenIsRejected() throws Exception {
        String rawToken = requestResetLink();

        PasswordResetToken token = tokenRepository
                .findByTokenHash(PasswordResetService.sha256Hex(rawToken)).orElseThrow();
        token.setExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));
        tokenRepository.save(token);

        postWithCsrf("/api/v1/auth/password/reset", resetJson(rawToken, NEW_PASSWORD), null)
                .andExpect(status().isBadRequest());

        AdminUser reloaded = adminUserRepository.findByUsername(username).orElseThrow();
        assertThat(passwordEncoder.matches(ORIGINAL_PASSWORD, reloaded.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("An unknown token is rejected generically")
    void unknownTokenIsRejected() throws Exception {
        postWithCsrf("/api/v1/auth/password/reset",
                resetJson("totally-made-up-token", NEW_PASSWORD), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Weak passwords are rejected on the server")
    void weakPasswordIsRejected() throws Exception {
        String rawToken = requestResetLink();

        postWithCsrf("/api/v1/auth/password/reset", resetJson(rawToken, "short1"), null)
                .andExpect(status().isBadRequest());

        AdminUser reloaded = adminUserRepository.findByUsername(username).orElseThrow();
        assertThat(passwordEncoder.matches(ORIGINAL_PASSWORD, reloaded.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Reset endpoints still require a CSRF token")
    void resetEndpointsRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgotJson(email)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetJson("some-token", NEW_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorization rules are unchanged: admin routes guarded, public routes open")
    void authorizationRulesAreUnchanged() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/plants"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/plants"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Responses never contain the token, hashes or SMTP settings")
    void responsesLeakNoSecrets() throws Exception {
        MvcResult forgot = postWithCsrf("/api/v1/auth/password/forgot", forgotJson(email), null)
                .andExpect(status().isAccepted())
                .andReturn();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce())
                .sendResetLink(anyString(), anyString(), tokenCaptor.capture(), any());

        String body = forgot.getResponse().getContentAsString();
        assertThat(body).doesNotContain(tokenCaptor.getValue());
        assertThat(body).doesNotContain("$2a$", "$2b$"); // bcrypt prefixes
        assertThat(body.toLowerCase()).doesNotContain("smtp", "password_hash", "token_hash");
    }

    // ------------------------------------------------------------- helpers

    /** Creates a reset link for the seeded admin and returns the raw token. */
    private String requestResetLink() throws Exception {
        postWithCsrf("/api/v1/auth/password/forgot", forgotJson(email), null)
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce())
                .sendResetLink(anyString(), anyString(), tokenCaptor.capture(), any());

        // Captures the most recently issued token.
        return tokenCaptor.getValue();
    }

    private void resetPassword(String rawToken, String newPassword) throws Exception {
        postWithCsrf("/api/v1/auth/password/reset", resetJson(rawToken, newPassword), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /** Performs a real login and returns the authenticated session. */
    private MockHttpSession login(String password) throws Exception {
        MockHttpSession session = new MockHttpSession();

        // With MockMvc the session object is supplied by the test, so no
        // Set-Cookie header is emitted; the authenticated /me call below is
        // what proves the session was established.
        postWithCsrf("/api/v1/auth/login", loginJson(password), session)
                .andExpect(status().isOk());

        return session;
    }

    /**
     * Sends a POST with a freshly fetched CSRF cookie/header pair, mirroring
     * exactly what the SPA does (both values must come from the same fetch).
     */
    private ResultActions postWithCsrf(String url, String json, MockHttpSession session)
            throws Exception {
        Csrf csrf = fetchCsrf();

        var request = post(url)
                .cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        if (session != null) {
            request = request.session(session);
        }

        return mockMvc.perform(request);
    }

    private Csrf fetchCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("XSRF-TOKEN cookie must be issued").isNotNull();
        return new Csrf(cookie, cookie.getValue());
    }

    private record Csrf(Cookie cookie, String token) {}

    private String forgotJson(String address) {
        return "{\"email\":\"" + address + "\"}";
    }

    private String resetJson(String token, String password) {
        return "{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}";
    }

    private String loginJson(String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }
}
