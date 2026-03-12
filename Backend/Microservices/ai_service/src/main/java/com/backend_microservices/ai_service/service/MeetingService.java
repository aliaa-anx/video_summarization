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

import java.io.File;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final TranscriptionClient transcriptionClient;
    private final MeetingTranscriptRepository transcriptRepo;
    private final SummaryService summaryService;
    private final SummaryRepository summaryRepo;

    public MeetingTranscript processMeeting(MultipartFile file, UUID userId) throws Exception {

        // we need this to convert our Spring's MultipartFile to java.io.File, that's what the Ai team needs
        File tempFile = File.createTempFile("meeting_", file.getOriginalFilename());
        file.transferTo(tempFile);      // the file is copied into a temp file on disk like: meeting_12345.mp3

        // here where our Ai team do their work ;)
        TranscriptionResponse transcription =
                transcriptionClient.processFile(tempFile);

        // the rest of the code doesn't even need to be explained...
        MeetingTranscript meeting = MeetingTranscript.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .transcript(transcription.getTranscript())
                .correctedTranscript(transcription.getCorrectedText())
                .createdAt(LocalDateTime.now())
                .build();

        transcriptRepo.save(meeting);

        return meeting;
    }

    public SummarizeResponse processMeetingThenSummarize(MultipartFile file, UUID userId) throws Exception {

        MeetingTranscript meeting = processMeeting(file, userId);

        SummarizeResponse summaryResponse = summaryService.summarizeText(meeting.getCorrectedTranscript());

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
}