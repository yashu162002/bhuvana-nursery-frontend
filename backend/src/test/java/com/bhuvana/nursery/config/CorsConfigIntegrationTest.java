package com.bhuvana.nursery.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS is opt-in: empty by default (same-origin proxy deployment) and exact
 * origins only when configured, because cookies travel with the requests.
 */
@SpringBootTest(properties = {
        "app.security.cors.allowed-origins=https://nursery.example.com"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigIntegrationTest {

    private static final String ORIGIN = "https://nursery.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromAnAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/plants")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void actualRequestFromAnAllowedOriginCarriesCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/plants")
                        .header(HttpHeaders.ORIGIN, ORIGIN))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));
    }

    @Test
    void preflightFromAnUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/plants")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
