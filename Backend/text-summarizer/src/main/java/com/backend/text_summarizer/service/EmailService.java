package com.backend.text_summarizer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    public void sendEmail(String to, String subject, String body) {
        // SIMULATION: This just prints to the console for testing
        System.out.println("================ SENDING EMAIL ================");
        System.out.println("TO: " + to);
        System.out.println("SUBJECT: " + subject);
        System.out.println("BODY: \n" + body);
        System.out.println("===============================================");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@your-app.com"); // Sender email (can be fake for Mailtrap)
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        System.out.println("Mail sent successfully to " + to);
    }
    public void sendBanNotification(String to,String name,Boolean isBanned){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Account Status Update");
        if(isBanned){
            message.setText("Hello " + name + "\",\\n\\nYour account has been suspended due to a violation of our terms.");
        }
        else {
            message.setText("Hello " + name + ",\n\nGood news! Your account suspension has been lifted. You can now log in.");
        }
        mailSender.send(message);
    }
}
