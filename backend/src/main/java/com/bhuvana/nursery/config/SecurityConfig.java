package com.bhuvana.nursery.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_USERNAME:nurseryadmin}") String username,
            @Value("${ADMIN_PASSWORD:Admin@Bhuvana2026}") String password
    ) {
        if (!StringUtils.hasText(username) ||
            !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                "Set ADMIN_USERNAME and ADMIN_PASSWORD before starting the backend."
            );
        }

        return new InMemoryUserDetailsManager(
            User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build()
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository
    ) throws Exception {

        CookieCsrfTokenRepository csrfRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        csrfRepository.setCookiePath("/");
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .sameSite("Lax")
                .secure(false)
        );

        http
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository)
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/csrf",
                    "/api/v1/auth/login",
                    "/actuator/health",
                    "/api/v1/health",
                    "/uploads/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/plants",
                    "/api/v1/plants/**",
                    "/api/v1/categories",
                    "/api/v1/hero-slides"
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId())
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                )
                .accessDeniedHandler((request, response, exception) ->
                    response.sendError(HttpServletResponse.SC_FORBIDDEN)
                )
            );

        return http.build();
    }

    /**
     * Supports the CSRF cookie/header pattern used by a React SPA.
     * Production should use HTTPS and Secure cookies.
     */
    static final class SpaCsrfTokenRequestHandler
            implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestAttributeHandler attributeHandler =
                new CsrfTokenRequestAttributeHandler();

        private final org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
                xorHandler =
                new org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken
        ) {
            this.xorHandler.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(
                HttpServletRequest request,
                CsrfToken csrfToken
        ) {
            String headerValue =
                    request.getHeader(csrfToken.getHeaderName());

            return (headerValue != null)
                    ? this.attributeHandler.resolveCsrfTokenValue(
                            request, csrfToken)
                    : this.xorHandler.resolveCsrfTokenValue(
                            request, csrfToken);
        }
    }
}
