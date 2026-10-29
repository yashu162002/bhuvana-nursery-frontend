package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.service.AdminSessionRegistry;
import com.bhuvana.nursery.service.ClientIpResolver;
import com.bhuvana.nursery.service.InMemoryRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String INVALID_CREDENTIALS_MESSAGE =
            "Invalid username or password.";

    static final String TOO_MANY_ATTEMPTS_MESSAGE =
            "Too many sign-in attempts. Please wait a few minutes and try again.";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AdminSessionRegistry sessionRegistry;
    private final ClientIpResolver clientIpResolver;
    private final InMemoryRateLimiter rateLimiter;

    private final int attemptsPerIp;
    private final int attemptsPerAccount;
    private final Duration attemptsWindow;
    private final boolean secureCookies;
    private final String cookieSameSite;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AdminSessionRegistry sessionRegistry,
            ClientIpResolver clientIpResolver,
            InMemoryRateLimiter rateLimiter,
            @Value("${app.auth.login-rate-limit.requests-per-ip:20}") int attemptsPerIp,
            @Value("${app.auth.login-rate-limit.requests-per-account:10}") int attemptsPerAccount,
            @Value("${app.auth.login-rate-limit.window-minutes:15}") long attemptsWindowMinutes,
            @Value("${app.security.cookie-secure:false}") boolean secureCookies,
            @Value("${app.security.cookie-same-site:Lax}") String cookieSameSite
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionRegistry = sessionRegistry;
        this.clientIpResolver = clientIpResolver;
        this.rateLimiter = rateLimiter;
        this.attemptsPerIp = attemptsPerIp;
        this.attemptsPerAccount = attemptsPerAccount;
        this.attemptsWindow = Duration.ofMinutes(attemptsWindowMinutes);
        this.secureCookies = secureCookies;
        this.cookieSameSite = cookieSameSite;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        if (request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username and password are required."));
        }

        String username = request.username().trim();

        if (username.isEmpty() || request.password().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username and password are required."));
        }

        // Throttle both by client address and by account so a single source
        // cannot spray passwords, and a leaked username cannot be brute forced
        // from many addresses at full speed.
        boolean withinBudget = rateLimiter.tryAcquire(
                "login-ip:" + clientIpResolver.resolve(servletRequest),
                attemptsPerIp,
                attemptsWindow
        );
        withinBudget = rateLimiter.tryAcquire(
                "login-account:" + username.toLowerCase(Locale.ROOT),
                attemptsPerAccount,
                attemptsWindow
        ) && withinBudget;

        if (!withinBudget) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", TOO_MANY_ATTEMPTS_MESSAGE));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            request.password()
                    )
            );

            // Session fixation protection for the JSON login flow: the
            // pre-login session id must not survive authentication.
            HttpSession session = servletRequest.getSession(true);
            servletRequest.changeSessionId();

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context, servletRequest, servletResponse
            );

            sessionRegistry.register(authentication.getName(), session);

            return ResponseEntity.ok(Map.of(
                    "username", authentication.getName(),
                    "role", "ADMIN"
            ));
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", INVALID_CREDENTIALS_MESSAGE));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "role", "ADMIN"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            if (authentication != null) {
                sessionRegistry.unregister(authentication.getName(), session);
            }

            session.invalidate();
        }

        response.addHeader(
                "Set-Cookie",
                "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite="
                        + cookieSameSite
                        + (secureCookies ? "; Secure" : "")
        );

        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    public record LoginRequest(String username, String password) {}
}
