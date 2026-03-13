package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.dto.AiResponse;
import com.backend_microservices.ai_service.dto.InitRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;


@Service


public class AiService {
    @Value("${python.ai.url}")
    private final String pythonNgrokUrl;

    private final RestTemplate restTemplate;

    // Manually write the constructor so you can place @Value correctly
    public AiService(
            @Value("${python.ai.url}") String pythonUrl,
            RestTemplate restTemplate) {
        this.pythonNgrokUrl = pythonUrl;
        this.restTemplate = restTemplate;
    }

    /**
     * New Method: Converts text into a vector (float array) using Python.
     * This is used by ChatService to perform local PGVector searches.
     */
    public float[] getEmbedding(String text) {
        // Matches a Python endpoint like: @app.post("/embed")
        String url = String.format("%s/embed", pythonNgrokUrl);

        // Wrap the text in a simple Map or a DTO
        Map<String, String> request = Map.of("text", text);

        // Python returns a list of floats, which RestTemplate maps to float[]
        return restTemplate.postForObject(url, request, float[].class);
    }

    /**
     * Sends the transcript to Python to be indexed for RAG.
     */
    public AiResponse initializeVideoContent(UUID chatId, String transcript) {
        String url = String.format("%s/chat/%s/init", pythonNgrokUrl, chatId);

        InitRequest request = new InitRequest(transcript);

        return restTemplate.postForObject(url, request, AiResponse.class);
    }

//    public String getReplyFromPython(UUID chatId, String message) {
//        String url = String.format("%s/chat/%s/ask", pythonUrl, chatId);
//        ChatRequest request = new ChatRequest(message);
//
//        // Sends JSON to Python and maps the response to our AiResponse DTO
//        AiResponse response = restTemplate.postForObject(url, request, AiResponse.class);
//        return response != null ? response.getReply() : "AI Error: No response";
//    }
}