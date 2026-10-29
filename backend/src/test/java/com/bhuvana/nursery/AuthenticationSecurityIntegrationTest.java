package com.bhuvana.nursery;

import com.bhuvana.nursery.entity.AdminUser;
import com.bhuvana.nursery.repository.AdminUserRepository;
import com.bhuvana.nursery.service.AdminSessionRegistry;
import com.bhuvana.nursery.service.InMemoryRateLimiter;
import com.bhuvana.nursery.service.PasswordResetEmailSender;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level coverage of the admin authentication hardening:
 * login throttling, session-id rotation, logout, and session revocation after
 * a password reset.
 */
@SpringBootTest(properties = {
        "app.auth.login-rate-limit.requests-per-ip=5",
        "app.auth.login-rate-limit.requests-per-account=3",
        "app.auth.login-rate-limit.window-minutes=15"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationSecurityIntegrationTest {

    private static final String PASSWORD = "OriginalPass123";
    private static final String NEW_PASSWORD = "RotatedPass456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InMemoryRateLimiter rateLimiter;

    @Autowired
    private AdminSessionRegistry sessionRegistry;

    @MockitoBean
    private PasswordResetEmailSender emailSender;

    private String username;
    private String email;

    @BeforeEach
    void setUp() {
        // Each test starts with a fresh throttle budget (the limiter is a
        // singleton shared by every request in this context).
        rateLimiter.reset();
        when(emailSender.isConfigured()).thenReturn(true);

        String unique = UUID.randomUUID().toString().substring(0, 8);
        username = "authadmin-" + unique;
        email = username + "@example.com";

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(username);
        adminUser.setPasswordHash(passwordEncoder.encode(PASSWORD));
        adminUser.setEmail(email);
        adminUser.setEnabled(true);
        adminUser.setPasswordChangedAt(Instant.now());
        adminUserRepository.save(adminUser);
    }

    @Test
    @DisplayName("A correct password signs in and rotates the session id")
    void loginRotatesSessionId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();

        login(session, PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertThat(session.getId())
                .as("session id must change during login to defeat session fixation")
                .isNotEqualTo(before);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    @DisplayName("A wrong password is rejected with a generic 401")
    void wrongPasswordIsRejected() throws Exception {
        login(new MockHttpSession(), "DefinitelyWrong123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    @DisplayName("An unknown username is rejected with the same generic 401")
    void unknownUsernameIsRejected() throws Exception {
        String unknownUsername = "missing-" + UUID.randomUUID();

        postWithCsrf(
                "/api/v1/auth/login",
                "{\"username\":\"" + unknownUsername + "\",\"password\":\"DefinitelyWrong123\"}",
                new MockHttpSession()
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    @DisplayName("Repeated failed sign-ins are throttled with 429")
    void repeatedFailuresAreThrottled() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            login(new MockHttpSession(), "DefinitelyWrong123")
                    .andExpect(status().isUnauthorized());
        }

        login(new MockHttpSession(), "DefinitelyWrong123")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Logging out invalidates the administrator session")
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(session, PASSWORD).andExpect(status().isOk());

        postWithCsrf("/api/v1/auth/logout", "{}", session)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Resetting the password revokes sessions that were already signed in")
    void passwordResetRevokesExistingSessions() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(session, PASSWORD).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk());
        assertThat(sessionRegistry.activeSessionCount(username)).isEqualTo(1);

        String rawToken = requestResetLink();
        postWithCsrf(
                "/api/v1/auth/password/reset",
                "{\"token\":\"" + rawToken + "\",\"password\":\"" + NEW_PASSWORD + "\"}",
                null
        ).andExpect(status().isOk());

        assertThat(sessionRegistry.activeSessionCount(username)).isZero();

        // The cookie from before the reset no longer grants access.
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/plants").session(session))
                .andExpect(status().isUnauthorized());

        // The new password works; the old one does not.
        login(new MockHttpSession(), NEW_PASSWORD).andExpect(status().isOk());
        login(new MockHttpSession(), PASSWORD).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- helpers

    private String requestResetLink() throws Exception {
        postWithCsrf(
                "/api/v1/auth/password/forgot",
                "{\"email\":\"" + email + "\"}",
                null
        ).andExpect(status().isAccepted());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce())
                .sendResetLink(anyString(), anyString(), tokenCaptor.capture(), any());

        return tokenCaptor.getValue();
    }

    private ResultActions login(MockHttpSession session, String password) throws Exception {
        return postWithCsrf(
                "/api/v1/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}",
                session
        );
    }

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
}
