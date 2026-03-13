package com.backend_microservices.ai_service.controller; // Update this to match your actual package name

import com.backend_microservices.ai_service.dto.ChatRequest;
import com.backend_microservices.ai_service.dto.InitRequest;
import com.backend_microservices.ai_service.service.ChatService;
import com.backend_microservices.ai_service.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai") // The base URL for all endpoints in this file
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows your Frontend to call this API without CORS errors
public class AiController {
    private final ChatService chatService;

    /**
     * Endpoint to initialize a chat with a video transcript.
     * URL: POST http://localhost:8083/api/ai/init/{userId}
     */
    @PostMapping("/chat/{chat_id}/init")
    public ResponseEntity<String> initChat(
            @PathVariable("chat_id") UUID userId,
            @RequestParam String title,
            @RequestBody InitRequest initRequest) {

        chatService.initializeChat(userId, title, initRequest.getTranscript());
        return ResponseEntity.ok("Chat initialized and transcript indexed successfully.");
    }
    /**
     * Endpoint to ask a question.
     * URL: POST http://localhost:8083/api/ai/chat/{chatId}
     */
    @PostMapping("/chat/{chatId}/ask")
    public ResponseEntity<String> askQuestion(
            @PathVariable UUID chatId,
            @RequestBody ChatRequest chatRequest) {

        // CHANGE THIS: Call askAi instead of processUserMessage
        String response = chatService.askAi(chatId, chatRequest.getMessage());
        return ResponseEntity.ok(response);
    }
    /**
     * Endpoint to retrieve chat history.
     * URL: GET http://localhost:8083/api/ai/history/{chatId}
     */
    @GetMapping("/history/{chatId}")
    public ResponseEntity<List<Message>> getHistory(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatHistory(chatId));
    }
}