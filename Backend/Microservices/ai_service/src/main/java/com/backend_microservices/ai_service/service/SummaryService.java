package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.client.SummarizationClient;
import com.backend_microservices.ai_service.client.TranscriptionClient;
import com.backend_microservices.ai_service.dto.*;

import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummarizationClient aiClient;
    private final SummaryRepository summaryRepo;
    private final TranscriptionClient transcriptionClient;
    private final MeetingTranscriptRepository transcriptRepo;



    public SummarizeResponse summarizeText(String transcript, MeetingTranscript meeting){
        /**
        SummarizeResponse summaryResponse =
                aiClient.summarize(transcript);

        return new SummarizeResponse(
                summaryResponse.getSummary(),
                summaryResponse.getLanguage()

        );**/
        // 1. Get the summary from the AI Model
        SummarizeResponse aiResponse = aiClient.summarize(transcript);

        // 2. Create the entity and save it to the DB
        Summary summary = Summary.builder()
                .id(UUID.randomUUID())
                .summary(aiResponse.getSummary())
                .language(aiResponse.getLanguage())
                .createdAt(LocalDateTime.now())
                .meeting(meeting) // Link to the transcript
                .title(meeting.getFileName()) // Use the real filename for the dashboard
                .build();

        summaryRepo.save(summary);

        return aiResponse;
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