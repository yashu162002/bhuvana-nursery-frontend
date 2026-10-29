package com.bhuvana.nursery.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves the client address used for rate limiting.
 *
 * <p>Forwarding headers are client-controlled unless a trusted reverse proxy
 * overwrites them, so they are only honoured when the deployment explicitly
 * enables {@code app.security.trust-forwarded-for} (env
 * {@code TRUST_FORWARDED_FOR}). With the default of {@code false} an attacker
 * cannot rotate a header to bypass the per-IP budget.</p>
 *
 * <p>When enabled, Netlify's {@code X-Nf-Client-Connection-Ip} is preferred:
 * Netlify sets it at the edge and it cannot be spoofed by the browser. For
 * other proxies the <em>last</em> {@code X-Forwarded-For} hop is used, because
 * that is the address appended by the proxy nearest to this application; the
 * first hop is supplied by the client and must never be trusted.</p>
 */
@Component
public class ClientIpResolver {

    private static final String NETLIFY_CLIENT_IP = "X-Nf-Client-Connection-Ip";
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final boolean trustForwardedFor;

    public ClientIpResolver(
            @Value("${app.security.trust-forwarded-for:false}") boolean trustForwardedFor
    ) {
        this.trustForwardedFor = trustForwardedFor;
    }

    public String resolve(HttpServletRequest request) {
        if (trustForwardedFor) {
            String netlifyClientIp = request.getHeader(NETLIFY_CLIENT_IP);
            if (StringUtils.hasText(netlifyClientIp)) {
                return netlifyClientIp.trim();
            }

            String forwarded = request.getHeader(FORWARDED_FOR);
            if (StringUtils.hasText(forwarded)) {
                int lastComma = forwarded.lastIndexOf(',');
                String nearestProxyHop = lastComma >= 0
                        ? forwarded.substring(lastComma + 1)
                        : forwarded;

                if (StringUtils.hasText(nearestProxyHop)) {
                    return nearestProxyHop.trim();
                }
            }
        }

        String remoteAddress = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddress) ? remoteAddress : "unknown";
    }
}
