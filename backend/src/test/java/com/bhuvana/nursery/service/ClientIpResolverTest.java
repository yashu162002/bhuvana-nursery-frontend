package com.bhuvana.nursery.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void ignoresForwardingHeadersWhenTrustIsDisabled() {
        MockHttpServletRequest request = requestWithRemoteAddress("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        request.addHeader("X-Nf-Client-Connection-Ip", "198.51.100.7");

        String resolved = new ClientIpResolver(false).resolve(request);

        assertThat(resolved).isEqualTo("10.0.0.5");
    }

    @Test
    void prefersNetlifyClientIpOverSpoofableForwardedFor() {
        MockHttpServletRequest request = requestWithRemoteAddress("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        request.addHeader("X-Nf-Client-Connection-Ip", "198.51.100.7");

        String resolved = new ClientIpResolver(true).resolve(request);

        assertThat(resolved).isEqualTo("198.51.100.7");
    }

    @Test
    void usesNearestForwardedHopForOtherTrustedProxies() {
        MockHttpServletRequest request = requestWithRemoteAddress("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 198.51.100.7");

        String resolved = new ClientIpResolver(true).resolve(request);

        assertThat(resolved).isEqualTo("198.51.100.7");
    }

    @Test
    void fallsBackToRemoteAddressWhenHeadersAreMissing() {
        MockHttpServletRequest request = requestWithRemoteAddress("10.0.0.5");

        String resolved = new ClientIpResolver(true).resolve(request);

        assertThat(resolved).isEqualTo("10.0.0.5");
    }

    @Test
    void ignoresBlankHeaderValuesAndUsesUnknownAsLastResort() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(" ");
        request.addHeader("X-Forwarded-For", " ");
        request.addHeader("X-Nf-Client-Connection-Ip", " ");

        String resolved = new ClientIpResolver(true).resolve(request);

        assertThat(resolved).isEqualTo("unknown");
    }

    private MockHttpServletRequest requestWithRemoteAddress(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }
}
