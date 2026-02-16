package com.backend_microservices.notification_service.service;

import com.backend_microservices.notification_service.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j

public class EmailService{
    private final JavaMailSender mailSender;
    public void sendEmail(EmailRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            // MAILTRAP CONFIGURATION:
            //
            // You can use ANY email address you want here, Mailtrap will accept it.
            message.setFrom("noreply@videosummary.com");

            message.setTo(request.getTo());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());

            mailSender.send(message);
            log.info("Dummy email sent to Mailtrap inbox for: {}", request.getTo());
        } catch (Exception e) {
            log.error("Failed to send email", e);
            throw new RuntimeException("Error sending email");
        }
    }

    public void sendBanNotification(String to, String username, boolean isBanned) {
        EmailRequest request = new EmailRequest();
        request.setTo(to);

        if (isBanned) {
            request.setSubject("Account Suspended");
            request.setBody("Hello " + username + ",\n\nYour account has been suspended.");
        } else {
            request.setSubject("Account Restored");
            request.setBody("Hello " + username + ",\n\nYour account is now active again!");
        }

        // Call your existing sendEmail method
        this.sendEmail(request);
    }

}
