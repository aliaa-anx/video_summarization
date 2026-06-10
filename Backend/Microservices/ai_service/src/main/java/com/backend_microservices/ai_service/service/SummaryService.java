package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.client.NotificationClient;
import com.backend_microservices.ai_service.client.SummarizationClient;
import com.backend_microservices.ai_service.client.UserClient;
import com.backend_microservices.ai_service.dto.*;

import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummarizationClient aiClient;
    private final SummaryRepository summaryRepo;
    private final NotificationClient notificationClient;
    private final UserClient userClient;

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

        SummarizeResponse summaryResponse = aiClient.summarizeExtractive(request);

        return summaryResponse;
    }

    public SummaryResponseAbstractive summarizeTextAbstractive(String transcript, MeetingTranscript meeting, String flag){
        SummaryResponseAbstractive aiResponse;

        if (flag.equals("long")){
             aiResponse = aiClient.summarizeAbstractiveLong(transcript);
        } else{
             aiResponse = aiClient.summarizeAbstractiveShort(transcript);
        }

        return aiResponse;
    }

    public byte[] convertSummaryToAudio(UUID meetingId, Boolean getNotified, UUID userId) {

        Summary summary = summaryRepo.findByMeeting_Id(meetingId);

        if (summary == null) {
            throw new RuntimeException("Summary not found");
        }

        String raw = summary.getSummaryJson();

        String speaker;

        if (summary.getLanguage().equals("english")) {
            speaker = "en-US-GuyNeural";
        } else {
            speaker = "ar-EG-ShakirNeural";
        }

        // remember that we have 2 types of summarization => extractive & abstractive, so the returned
        // summaries are not the same, thats why i needed to implement the next function
        String text = getText(raw);

        byte[] generatedAudio = aiClient.generateAudio(text, speaker);

        if(getNotified == true){
            UserDto user = userClient.getUserById(userId);
            notificationClient.sendSummaryEmail(user.getEmail(), user.getUsername(), "Your Audio Summary is Ready");
        }

        return generatedAudio;
    }


    private String getText(String raw) {
        // here this function does extra processing on the extractive summaries (because of the timestamps TwT)

        if (raw == null || raw.isBlank()) {
            return "";
        }

        // in case the summary has '[' then its JSON so we need to extract the actual summary text out of it
        if (raw.trim().startsWith("[")) {
            try {
                // converting JSON to java object => List
                ObjectMapper objectMapper = new ObjectMapper();
                List<SegmentDto> segments = objectMapper.readValue(
                        raw,
                        new TypeReference<List<SegmentDto>>() {}
                );

                // we need to sort based on the timestamps in case if the input is video/audio
                // and wee need to soft based on the index if the input is textfile
                return IntStream.range(0, segments.size())
                        .boxed()
                        .sorted(
                                Comparator.comparingDouble(i ->
                                        !Double.isNaN(segments.get(i).getStart())
                                                ? segments.get(i).getStart()  // sort based on timestamps
                                                : i     // sort based on the index
                                )
                        )
                        .map(i -> segments.get(i).getText())  // return the summary text from the list
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "));

            } catch (Exception e) {
                throw new RuntimeException("Invalid segment JSON format", e);
            }
        }
        return raw;
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