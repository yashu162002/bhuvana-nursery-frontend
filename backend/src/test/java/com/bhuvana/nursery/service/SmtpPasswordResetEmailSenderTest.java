package com.bhuvana.nursery.service;

import jakarta.mail.Address;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the real mail body without a network: the sender is pointed at a
 * mocked {@link JavaMailSender}, so the test can inspect the exact MIME message
 * that Gmail would receive.
 */
class SmtpPasswordResetEmailSenderTest {

    private static final String FROM = "nursery.admin@example.com";
    private static final String BASE_URL = "http://localhost:5199";
    private static final String HOST = "smtp.gmail.com";
    private static final String TOKEN = "raw-token-abc";

    private JavaMailSender mailSender;
    private MimeMessage message;
    private ObjectProvider<JavaMailSender> provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
    }

    @Test
    @DisplayName("Mail counts as configured only with a host, a sender and a mail sender")
    void isConfiguredRequiresHostSenderAndMailSender() {
        assertThat(sender(FROM, HOST).isConfigured()).isTrue();
        assertThat(sender(FROM, "").isConfigured()).isFalse();
        assertThat(sender("", HOST).isConfigured()).isFalse();

        when(provider.getIfAvailable()).thenReturn(null);

        assertThat(sender(FROM, HOST).isConfigured()).isFalse();
    }

    @Test
    @DisplayName("The message carries the expected subject, recipient and reset link")
    void sendsExpectedSubjectAndResetLink() throws Exception {
        sender(FROM, HOST).sendResetLink(
                "admin@example.com", "nurseryadmin", TOKEN, Duration.ofMinutes(20));

        verify(mailSender).send(message);
        // The real transport calls this before sending; without it the
        // in-memory message still carries default Content-Type headers.
        message.saveChanges();

        assertThat(message.getSubject())
                .isEqualTo("Reset your Bhuvana Nursery admin password");
        assertThat(addresses(message.getAllRecipients())).containsExactly("admin@example.com");
        assertThat(addresses(message.getFrom())).containsExactly(FROM);

        String link = BASE_URL + "/admin/reset-password?token=" + TOKEN;
        String plain = textPart("text/plain");
        String html = textPart("text/html");

        assertThat(plain).contains("nurseryadmin", link, "20 minutes");
        assertThat(html).contains(link).contains("Reset my password");
    }

    @Test
    @DisplayName("A trailing slash on the frontend URL does not produce a double slash")
    void trimsTrailingSlashFromBaseUrl() throws Exception {
        new SmtpPasswordResetEmailSender(provider, FROM, BASE_URL + "/", HOST)
                .sendResetLink(FROM, "nurseryadmin", TOKEN, Duration.ofMinutes(20));
        message.saveChanges();

        assertThat(textPart("text/plain"))
                .contains(BASE_URL + "/admin/reset-password?token=" + TOKEN)
                .doesNotContain(BASE_URL + "//admin");
    }

    @Test
    @DisplayName("The administrator name is HTML-escaped in the rich part")
    void escapesAdministratorNameInHtml() throws Exception {
        sender(FROM, HOST).sendResetLink(
                FROM, "Nursery <Admin>", TOKEN, Duration.ofMinutes(20));
        message.saveChanges();

        assertThat(textPart("text/html"))
                .contains("Nursery &lt;Admin&gt;")
                .doesNotContain("Nursery <Admin>");
    }

    @Test
    @DisplayName("An unconfigured sender refuses to send instead of pretending")
    void refusesToSendWhenUnconfigured() {
        SmtpPasswordResetEmailSender unconfigured = sender(FROM, "");

        assertThatThrownBy(() -> unconfigured.sendResetLink(
                FROM, "nurseryadmin", TOKEN, Duration.ofMinutes(20)))
                .isInstanceOf(PasswordResetEmailException.class)
                .hasMessageContaining("not configured");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private SmtpPasswordResetEmailSender sender(String from, String host) {
        return new SmtpPasswordResetEmailSender(provider, from, BASE_URL, host);
    }

    /**
     * Walks the nested MIME tree (Jakarta Mail wraps the text/plain +
     * text/html alternative inside another multipart) and returns the first
     * body part of the requested type.
     */
    private String textPart(String mimeType) throws Exception {
        return findPart(message, mimeType);
    }

    private static String findPart(Part part, String mimeType) throws Exception {
        Object content = part.getContent();

        if (part.isMimeType(mimeType) && content instanceof String text) {
            return text;
        }

        if (content instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                String match = findPart(multipart.getBodyPart(index), mimeType);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    private static List<String> addresses(Address[] addresses) {
        return Arrays.stream(addresses).map(Address::toString).toList();
    }
}
