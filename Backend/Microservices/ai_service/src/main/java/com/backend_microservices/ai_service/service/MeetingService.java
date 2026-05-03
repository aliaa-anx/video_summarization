package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.client.TranscriptionClient;
import com.backend_microservices.ai_service.dto.SummarizeResponse;
import com.backend_microservices.ai_service.dto.TranscriptionResponse;
import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final TranscriptionClient transcriptionClient;
    private final MeetingTranscriptRepository transcriptRepo;
    private final SummaryService summaryService;
    private final SummaryRepository summaryRepo;

//    public MeetingTranscript processMeeting(MultipartFile file, UUID userId) throws Exception {
//
//        // we need this to convert our Spring's MultipartFile to java.io.File, that's what the Ai team needs
//        File tempFile = File.createTempFile("meeting_", file.getOriginalFilename());
//        file.transferTo(tempFile);      // the file is copied into a temp file on disk like: meeting_12345.mp3
//
//        // here where our Ai team do their work ;)
//        TranscriptionResponse transcription =
//                transcriptionClient.processFile(tempFile);
//
//        // the rest of the code doesn't even need to be explained...
//        MeetingTranscript meeting = MeetingTranscript.builder()
//                .id(UUID.randomUUID())
//                .userId(userId)
//                .fileName(file.getOriginalFilename())
//                .transcript(transcription.getTranscript())
//                .correctedTranscript(transcription.getCorrectedText())
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        transcriptRepo.save(meeting);
//
//        return meeting;
//    }

    public MeetingTranscript processMeeting(MultipartFile file, UUID userId) throws Exception {

        File tempFile = File.createTempFile("meeting_", file.getOriginalFilename());
        file.transferTo(tempFile);

        TranscriptionResponse transcription = transcriptionClient.processFile(tempFile);

        // source comes directly from AI team response
        String source = transcription.getSource();

        // Merge and save segments as JSON if audio
        String segmentsJson = null;
        if ("audio".equals(source) && transcription.getSegments() != null
                && !transcription.getSegments().isEmpty()) {
            List<TranscriptionResponse.Segment> merged =
                    mergeSegments(transcription.getSegments(), 1.0);
            segmentsJson = new ObjectMapper().writeValueAsString(merged);
        }

        MeetingTranscript meeting = MeetingTranscript.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .transcript(transcription.getTranscript())
                .correctedTranscript(transcription.getCorrectedText())
                .source(source)          // 👈 from AI response
                .segmentsJson(segmentsJson)
                .createdAt(LocalDateTime.now())
                .build();

        transcriptRepo.save(meeting);
        return meeting;
    }

    private List<TranscriptionResponse.Segment> mergeSegments(
            List<TranscriptionResponse.Segment> segments, double maxGap) {

        List<TranscriptionResponse.Segment> merged = new ArrayList<>();
        TranscriptionResponse.Segment current = segments.get(0);

        for (int i = 1; i < segments.size(); i++) {
            TranscriptionResponse.Segment next = segments.get(i);
            double gap = next.getStart() - current.getEnd();

            if (gap <= maxGap) {
                current.setEnd(next.getEnd());
                current.setText(current.getText() + " " + next.getText());
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }





    public SummarizeResponse processMeetingThenSummarize(MultipartFile file, UUID userId) throws Exception {

        MeetingTranscript meeting = processMeeting(file, userId);

        SummarizeResponse summaryResponse = summaryService.summarizeText(meeting.getCorrectedTranscript(), meeting);


        Summary summary = Summary.builder()
                .id(UUID.randomUUID())
                .summary(summaryResponse.getSummary())
                .language(summaryResponse.getLanguage())
                .createdAt(LocalDateTime.now())
                .meeting(meeting)
                .title("Untitled")
                .build();

        summaryRepo.save(summary);

        return summaryResponse;
   }

    public MeetingTranscript findById(UUID meetingId) {
        return transcriptRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
    }
}