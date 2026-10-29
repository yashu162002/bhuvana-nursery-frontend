package com.bhuvana.nursery.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sends the reset link through Gmail SMTP (or any spring.mail-configured
 * server) using an App Password, never the account password.
 *
 * <p>Neither the raw token nor the reset link is ever written to the logs.</p>
 */
@Component
public class SmtpPasswordResetEmailSender implements PasswordResetEmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;
    private final String baseUrl;
    private final String smtpHost;

    public SmtpPasswordResetEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.password-reset.mail.from:}") String fromAddress,
            @Value("${app.password-reset.base-url}") String baseUrl,
            @Value("${spring.mail.host:}") String smtpHost
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
        this.smtpHost = smtpHost;
    }

    @Override
    public boolean isConfigured() {
        return mailSenderProvider.getIfAvailable() != null
                && StringUtils.hasText(fromAddress)
                && StringUtils.hasText(smtpHost);
    }

    @Override
    public void sendResetLink(String toEmail, String username, String rawToken, Duration ttl) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null
                || !StringUtils.hasText(fromAddress)
                || !StringUtils.hasText(smtpHost)) {
            throw new PasswordResetEmailException("SMTP is not configured.");
        }

        long minutes = Math.max(1, ttl.toMinutes());
        String resetLink = buildResetLink(rawToken);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Reset your Bhuvana Nursery admin password");
            helper.setText(
                    plainText(username, resetLink, minutes),
                    htmlText(htmlEscape(username), resetLink, minutes)
            );

            mailSender.send(message);
        } catch (PasswordResetEmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PasswordResetEmailException("Failed to send the reset email.", ex);
        }
    }

    private String buildResetLink(String rawToken) {
        String base = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return base + "/admin/reset-password?token=" + rawToken;
    }

    /** Administrator names are operator-controlled, but escape them anyway. */
    private String htmlEscape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String plainText(String username, String resetLink, long minutes) {
        return """
                Hello %s,

                A password reset was requested for your Bhuvana Nursery administrator
                account. Open the link below to choose a new password:

                %s

                The link expires in %d minutes and can only be used once.

                If you did not request this, you can safely ignore this email. Your
                password will not change until you confirm a new one through the link.
                """.formatted(username, resetLink, minutes);
    }

    private String htmlText(String username, String resetLink, long minutes) {
        return """
                <html><body style="font-family: Arial, Helvetica, sans-serif; color:#1d2420;">
                <p>Hello %s,</p>
                <p>A password reset was requested for your <strong>Bhuvana Nursery</strong>
                administrator account.</p>
                <p><a href="%s"
                      style="display:inline-block;padding:12px 20px;background:#2f6b3a;
                             color:#ffffff;text-decoration:none;border-radius:8px;">
                   Reset my password</a></p>
                <p style="font-size:13px;color:#5b6660;">This link expires in %d minutes
                and can only be used once. If you did not request it, ignore this email —
                your password stays unchanged.</p>
                </body></html>
                """.formatted(username, resetLink, minutes);
    }
}
