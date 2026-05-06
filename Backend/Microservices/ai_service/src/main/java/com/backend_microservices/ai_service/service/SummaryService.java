package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.client.ExtractiveSummarizationClient;
import com.backend_microservices.ai_service.client.TranscriptionClient;
import com.backend_microservices.ai_service.dto.*;

import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ExtractiveSummarizationClient aiClient;
    private final SummaryRepository summaryRepo;
    private final TranscriptionClient transcriptionClient;
    private final MeetingTranscriptRepository transcriptRepo;


    public SummarizeResponse summarizeTextExtractive(MeetingDto meetingDto) throws JsonProcessingException {

        // used to convert between Java objects and JSON
        ObjectMapper mapper = new ObjectMapper();

        // remember that the user can upload text not just video, so text doesn't have timestamps of,course :)
        List<SegmentDto> segments = Optional.ofNullable(meetingDto.getSegmentsJson())
                .filter(s -> !s.isBlank())
                .map(json -> {
                    try {
                        // this is a Jackson method that converts JSON to Java objects
                        return mapper.readValue(json, new TypeReference<List<SegmentDto>>() {});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(Collections.emptyList());   // if getSegmentsJson() returned null, meaning its text so it has no segments

        // building the request that we will send to the ai
        SummarizeRequest request = SummarizeRequest.builder()
                .transcript(meetingDto.getTranscript())
                .source(meetingDto.getSource())
                .correctedTranscript(meetingDto.getCorrected_text())
                .segments(segments)
                .status("success")
                .build();

        SummarizeResponse summaryResponse = aiClient.summarize(request);

        return summaryResponse;
    }


    public String extractTitle(String summary) {

        if (summary == null || summary.isBlank()) {
            return "Untitled";
        }

        Pattern pattern = Pattern.compile("^\\s*##\\s*(.*?)\\s*##");
        Matcher matcher = pattern.matcher(summary);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "Untitled";
    }
}