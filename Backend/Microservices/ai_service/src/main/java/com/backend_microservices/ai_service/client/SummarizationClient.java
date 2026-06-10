package com.backend_microservices.ai_service.client;

import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// Calling the AI  APIs
@Service
public class SummarizationClient {
    private final MeetingTranscriptRepository transcriptRepo;
    private final SummaryRepository summary;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String Summarization_URL_EXTRACTIVE = "https://geiger-opium-handiness.ngrok-free.dev/summarize";
    private final String Reconstruction_URL = "https://geiger-opium-handiness.ngrok-free.dev/reconstruct";
    private final String Summarization_URL_ABSTRACTIVE_LONG = "https://excretory-agnostic-pacifism.ngrok-free.dev/long_summary";
    private final String Summarization_URL_ABSTRACTIVE_SHORT = "https://excretory-agnostic-pacifism.ngrok-free.dev/short_summary";
    private final String Summarization_AUDIO_SUMMARY = "https://growlingly-ponderous-leah.ngrok-free.dev/generate-audio/";

    public SummarizationClient(MeetingTranscriptRepository transcriptRepo, SummaryRepository summary) {
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

    public SummarizeResponse summarizeExtractive(SummarizeRequest request){

        return restTemplate.postForObject(
                Summarization_URL_EXTRACTIVE,
                request,
                SummarizeResponse.class
        );
    }

    public SummaryResponseAbstractive summarizeAbstractiveLong(String transcript){

        Map<String, String> request = new HashMap<>();
        request.put("transcript", transcript);

        return restTemplate.postForObject(
                Summarization_URL_ABSTRACTIVE_LONG,
                request,
                SummaryResponseAbstractive.class
        );
    }

    public SummaryResponseAbstractive summarizeAbstractiveShort(String transcript){

        Map<String, String> request = new HashMap<>();
        request.put("transcript", transcript);

        return restTemplate.postForObject(
                Summarization_URL_ABSTRACTIVE_SHORT,
                request,
                SummaryResponseAbstractive.class
        );
    }


        public byte[] generateAudio(String text, String speaker) {

            AudioSummaryRequest request = AudioSummaryRequest.builder()
                    .text(text)
                    .speaker(speaker)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AudioSummaryRequest> entity =
                    new HttpEntity<>(request, headers);

            ResponseEntity<byte[]> response =
                    restTemplate.exchange(
                            Summarization_AUDIO_SUMMARY,
                            HttpMethod.POST,
                            entity,
                            byte[].class
                    );

            return response.getBody();
        }

}