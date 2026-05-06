package com.backend_microservices.ai_service.client;

import com.backend_microservices.ai_service.dto.TranscriptionResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

// Calling the AI  APIs
@Service
public class TranscriptionClient {

    // this is a Spring HTTP client used to call external APIs
    private final RestTemplate restTemplate = new RestTemplate();

    private final String TRANSCRIPTION_URL = "https://scared-finch-briskness.ngrok-free.dev/process-file";

    // this method receives Java File object (java.io.File) like: meeting_12345.mp3
    public TranscriptionResponse processFile(File file) {

        //here we create the request body, multipart/form-data requests contains multiple fields
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // this converts a Java File into something Spring can send over HTTP
        FileSystemResource resource = new FileSystemResource(file) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        };

        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);  // this tells the API that the request contains a file upload

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<TranscriptionResponse> response =
                restTemplate.postForEntity(
                        TRANSCRIPTION_URL,          // that's the AI endpoint
                        request,                    // that's the request that contains heady, body & file
                        TranscriptionResponse.class // that's the response type that we want to be returned
                );

        return response.getBody();
    }
}