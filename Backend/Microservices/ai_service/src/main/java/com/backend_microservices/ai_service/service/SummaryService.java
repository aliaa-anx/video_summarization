package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.client.SummarizationClient;
import com.backend_microservices.ai_service.dto.*;

import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummarizationClient aiClient;

    public SummarizeResponse summarizeText(String transcript){

        SummarizeResponse summaryResponse =
                aiClient.summarize(transcript);

        return new SummarizeResponse(
                summaryResponse.getSummary(),
                summaryResponse.getLanguage()

        );
    }

//    public String extractTitle(String summary) {
//
//        if (summary == null) return "Untitled";
//
//        int start = summary.indexOf("##");
//        if (start == -1) return "Untitled";
//
//        start += 2; // skip the first ##
//
//        int end = summary.indexOf("##", start);
//        if (end == -1) return "Untitled";
//
//        return summary.substring(start, end).trim();
//    }

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