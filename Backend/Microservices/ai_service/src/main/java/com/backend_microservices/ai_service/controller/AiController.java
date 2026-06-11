package com.backend_microservices.ai_service.controller;

import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import com.backend_microservices.ai_service.service.ChatService;
import com.backend_microservices.ai_service.service.MeetingService;
import com.backend_microservices.ai_service.service.SummaryService;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final MeetingService meetingService;
    private final SummaryService summaryService;
    private final ChatService chatService;
    private final SummaryRepository summaryRepo;

//    @PostMapping("/upload")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public MeetingTranscript uploadMeeting(
//            @RequestParam("file") MultipartFile file,
//            @RequestHeader("X-User-Id") String userId
//    ) throws Exception {
//
//        return meetingService.processMeetingExtractive(
//                file,
//                UUID.fromString(userId)
//        );
//    }


    @PostMapping("/upload-summarize-extractive")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public SummarizeResponseWithMeetingId uploadThenSummarizeExtractive(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean getNotified,
            @RequestHeader("X-User-Id") UUID userId
    ) throws Exception {

        return meetingService.processMeetingThenSummarizeExtractive(file, userId, getNotified);
    }


    @GetMapping(value = "/reconstruct/{meetingId}", produces = "video/mp4")
    public ResponseEntity<Resource> reconstruct(
            @PathVariable UUID meetingId,
            @RequestParam(defaultValue = "false") boolean getNotified,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        byte[] videoBytes = meetingService.reconstructMeeting(meetingId, getNotified, userId);
        ByteArrayResource resource = new ByteArrayResource(videoBytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=reconstructed_" + meetingId + ".mp4")
                .contentType(MediaType.valueOf("video/mp4"))
                .contentLength(videoBytes.length)
                .body(resource);
    }

    @PostMapping("/upload-summarize-abstractive/{flag}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public SummaryResponseAbsWithMeetingId uploadThenSummarizeAbstractive(
            @PathVariable String flag,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "false") boolean getNotified
    ) throws Exception {

        return meetingService.processMeetingThenSummarizeAbstractive(file, userId, flag, getNotified);
    }
//        @PostMapping("/upload-summarize-abstractive/{flag}")
//        @PreAuthorize("hasAuthority('ROLE_USER')")
//        public SummaryResponseAbsWithMeetingId uploadThenSummarizeAbstractive(
//                @PathVariable String flag,
//                @RequestParam("file") MultipartFile file,
//                @RequestHeader("X-User-Id") UUID userId,
//                @RequestParam(value = "videoDurationMinutes", required = false) Double videoDurationMinutes,
//                @RequestParam(value = "targetSummaryMinutes", required = false) Double targetSummaryMinutes
//        ) throws Exception {
//
//            return meetingService.processMeetingThenSummarizeAbstractive(file, userId, flag, videoDurationMinutes, targetSummaryMinutes);
//        }

    @GetMapping(value = "/summary-to-audio/{meetingId}", produces = "audio/mpeg")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<byte[]> summaryToAudio(
            @PathVariable UUID meetingId,
            @RequestParam(defaultValue = "false") boolean getNotified,
            @RequestHeader("X-User-Id") UUID userId
    ) {

        byte[] audio = summaryService.convertSummaryToAudio(meetingId, getNotified, userId);
        String randomFilename = UUID.randomUUID().toString() + ".mp3";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=" + randomFilename)
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(audio);
    }

    @GetMapping("/get-meeting-transcript/{meetingId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public CorrectedTranscriptDto getMeetingTranscript(
            @PathVariable UUID meetingId
    ) throws Exception {
        MeetingTranscript meeting = meetingService.findById(meetingId);
        CorrectedTranscriptDto correctedTranscript = new CorrectedTranscriptDto();
        correctedTranscript.setCorrectedTranscript(meeting.getCorrectedTranscript());
        return correctedTranscript;
    }


    /**
     * Endpoint to initialize a chat.
     * Returns the conversationId used for subsequent /ask calls.
     */
    @PostMapping("/chat/{meetingId}/init")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<Map<String, String>> initChat(
            @PathVariable UUID meetingId,
            @RequestHeader("X-User-Id") String userId) {

        MeetingTranscript meeting = meetingService.findById(meetingId);

        // Pass 'meetingId' as the second argument
        String conversationId = chatService.initializeChat(
                UUID.fromString(userId),
                meetingId, //
                meeting.getFileName(),
                meeting.getCorrectedTranscript(),
                meeting.getSource(),
                meeting.getSegmentsJson()
        );

        return ResponseEntity.ok(Map.of("conversationId", conversationId));
    }


    @PostMapping("/chat/{chatId}/ask")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<Map<String, String>> askQuestion(@PathVariable UUID chatId,
            @RequestBody ChatRequest chatRequest) {
        String response = chatService.askAi(chatId, chatRequest.getMessage());

        // Fix: Use a HashMap because Map.of() crashes on nulls
        Map<String, String> body = new HashMap<>();
        body.put("reply", response != null ? response : "AI Error: Received null reply");

        return ResponseEntity.ok(body);
    }


    @GetMapping("/history")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<List<ChatHistoryDto>> getHistory(@RequestHeader("X-User-Id") String userId) {
        UUID userUuid = UUID.fromString(userId);
        // This will return an array of ChatHistoryDto objects
        return ResponseEntity.ok(chatService.getGroupedChatHistory(userUuid));
    }


}