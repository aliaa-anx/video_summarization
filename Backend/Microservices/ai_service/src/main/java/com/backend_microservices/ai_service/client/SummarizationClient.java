package com.backend_microservices.ai_service.client;

import com.backend_microservices.ai_service.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// Calling the AI  APIs
@Service
public class SummarizationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String Summarization_URL = "https://growlingly-ponderous-leah.ngrok-free.dev/summarize";

    public SummarizeResponse summarize(String transcript){

        SummarizeRequest request =
                new SummarizeRequest(transcript);

        return restTemplate.postForObject(
                Summarization_URL,
                request,
                SummarizeResponse.class
        );
    }
}