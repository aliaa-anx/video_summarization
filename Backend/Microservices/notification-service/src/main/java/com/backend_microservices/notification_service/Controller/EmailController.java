package com.backend_microservices.notification_service.Controller;

import com.backend_microservices.notification_service.dto.EmailRequest;
import com.backend_microservices.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
public class EmailController {
    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendEmail(@RequestBody EmailRequest emailRequest) {
        log.info("Received email request for: {}", emailRequest.getTo());

        emailService.sendEmail(emailRequest);

        //  JSON response
        Map<String, String> response = new HashMap<>();
        response.put("message", "Email sent successfully");
        response.put("status", "success");

        // Return HTTP 200 OK with the JSON body
        return ResponseEntity.ok(response);
    }



        @PostMapping("/ban-email")
        public void sendBanEmail(@RequestParam String email,
                                 @RequestParam String username,
                                 @RequestParam boolean isBanned) {

            // Pass the data to the service logic
            emailService.sendBanNotification(email, username, isBanned);
        }

    @PostMapping("/summary-email")
    public void sendSummaryEmail(@RequestParam String email,
                             @RequestParam String username,
                             @RequestParam String message
    ) {

        // Pass the data to the service logic
        emailService.sendAiNotification(email, username, message);
    }
    }


