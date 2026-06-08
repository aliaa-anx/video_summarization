package com.backend_microservices.notification_service.service;

import com.backend_microservices.notification_service.dto.EmailRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests — EmailService
 *
 * Mocked dependencies:
 *   - JavaMailSender → Spring mail abstraction (SMTP / Mailtrap)
 *
 * No real emails are sent — JavaMailSender is mocked so send() is intercepted.
 * ArgumentCaptor is used to inspect the SimpleMailMessage that was built and passed
 * to mailSender.send(), letting us assert on to/subject/body without a real mail server.
 *
 * Key areas covered:
 *   - sendEmail           → happy path + failure propagation
 *   - sendBanNotification → banned branch + restored branch
 */
@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    // ── Mocked dependencies ────────────────────────────────────────────────
    @Mock private JavaMailSender mailSender;

    // ── System Under Test ─────────────────────────────────────────────────
    @InjectMocks
    private EmailService emailService;


    // ── Helpers ───────────────────────────────────────────────────────────

    /** Builds a minimal EmailRequest. */
    private EmailRequest makeRequest(String to, String subject, String body) {
        EmailRequest r = new EmailRequest();
        r.setTo(to);
        r.setSubject(subject);
        r.setBody(body);
        return r;
    }


    // ═══════════════════════════════════════════════════════════
    //  SEND EMAIL
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — valid request.
     * Verifies mailSender.send() is called exactly once and the SimpleMailMessage
     * is assembled with the correct from, to, subject, and body.
     */
    @Test
    void testSendEmail_Success() {
        // 1. Arrange
        EmailRequest request = makeRequest(
                "user@example.com",
                "Your summary is ready",
                "Hello, your meeting summary has been generated."
        );

        // Capture the SimpleMailMessage that gets passed to send()
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // mailSender.send() is void — no stubbing needed, Mockito ignores it by default

        // 2. Act
        assertDoesNotThrow(() -> emailService.sendEmail(request));

        // 3. Assert — send() called once with correctly built message
        Mockito.verify(mailSender, Mockito.times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("noreply@videosummary.com", sent.getFrom());
        assertEquals("user@example.com", sent.getTo()[0]);
        assertEquals("Your summary is ready", sent.getSubject());
        assertEquals("Hello, your meeting summary has been generated.", sent.getText());
    }

    /**
     * Failure — mailSender.send() throws (SMTP error, Mailtrap down, etc.).
     * The catch block must rethrow as RuntimeException("Error sending email").
     * Verifies the message and that send() was indeed attempted.
     */
    @Test
    void testSendEmail_Failure_MailSenderThrows() {
        // 1. Arrange
        EmailRequest request = makeRequest(
                "user@example.com", "Subject", "Body"
        );

        Mockito.doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailSender).send(Mockito.any(SimpleMailMessage.class));

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                emailService.sendEmail(request)
        );
        assertEquals("Error sending email", ex.getMessage());

        // Confirm send() was attempted before it threw
        Mockito.verify(mailSender, Mockito.times(1))
                .send(Mockito.any(SimpleMailMessage.class));
    }


    // ═══════════════════════════════════════════════════════════
    //  SEND BAN NOTIFICATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — isBanned = true.
     * Verifies subject is "Account Suspended" and body contains the username.
     * Also verifies the correct recipient is used.
     */
    @Test
    void testSendBanNotification_Success_UserIsBanned() {
        // 1. Arrange
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // 2. Act
        emailService.sendBanNotification("user@example.com", "john_doe", true);

        // 3. Assert
        Mockito.verify(mailSender, Mockito.times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("user@example.com", sent.getTo()[0]);
        assertEquals("Account Suspended", sent.getSubject());
        assertTrue(sent.getText().contains("john_doe"));
        assertTrue(sent.getText().contains("suspended"));
    }

    /**
     * Happy path — isBanned = false (account restored).
     * Verifies subject is "Account Restored" and body contains the username.
     */
    @Test
    void testSendBanNotification_Success_UserIsRestored() {
        // 1. Arrange
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // 2. Act
        emailService.sendBanNotification("user@example.com", "jane_doe", false);

        // 3. Assert
        Mockito.verify(mailSender, Mockito.times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("user@example.com", sent.getTo()[0]);
        assertEquals("Account Restored", sent.getSubject());
        assertTrue(sent.getText().contains("jane_doe"));
        assertTrue(sent.getText().contains("active"));
    }
}