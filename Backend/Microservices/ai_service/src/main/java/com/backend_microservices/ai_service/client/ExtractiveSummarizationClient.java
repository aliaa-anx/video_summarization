package com.backend_microservices.ai_service.client;

import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Calling the AI  APIs
@Service
public class ExtractiveSummarizationClient {
    private final MeetingTranscriptRepository transcriptRepo;
    private final SummaryRepository summary;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String Summarization_URL = "https://geiger-opium-handiness.ngrok-free.dev/summarize";
    private final String Reconstruction_URL = "https://geiger-opium-handiness.ngrok-free.dev/reconstruct";

    public ExtractiveSummarizationClient(MeetingTranscriptRepository transcriptRepo, SummaryRepository summary) {
        this.transcriptRepo = transcriptRepo;
        this.summary = summary;
    }

    public byte[] reconstructVideo(File file, String keypointsJson) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("keypoints", keypointsJson);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response =
                restTemplate.postForEntity(
                        Reconstruction_URL,
                        request,
                        byte[].class    // returns the bytes of the reconstructed video
                );

        return response.getBody();
    }

    public SummarizeResponse summarize(SummarizeRequest request){

        return restTemplate.postForObject(
                Summarization_URL,
                request,
                SummarizeResponse.class
        );
    }

}