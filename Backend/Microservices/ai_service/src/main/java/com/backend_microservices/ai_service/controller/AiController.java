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
@CrossOrigin(origins = "*") // Allows your Frontend to call this API without CORS errors
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
            @RequestHeader("X-User-Id") UUID userId
    ) throws Exception {

        return meetingService.processMeetingThenSummarizeExtractive(file, userId);
    }


    @GetMapping(value = "/reconstruct/{meetingId}", produces = "video/mp4")
    public ResponseEntity<Resource> reconstruct(@PathVariable UUID meetingId) {
        byte[] videoBytes = meetingService.reconstructMeeting(meetingId);
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
            @RequestHeader("X-User-Id") UUID userId
    ) throws Exception {

        return meetingService.processMeetingThenSummarizeAbstractive(file, userId, flag);
    }

    @GetMapping(value = "/summary-to-audio/{meetingId}", produces = "audio/mpeg")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<byte[]> summaryToAudio(
            @PathVariable UUID meetingId
    ) {

        byte[] audio = summaryService.convertSummaryToAudio(meetingId);
        String randomFilename = UUID.randomUUID().toString() + ".mp3";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=" + randomFilename)
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(audio);
    }

    /**
     * Endpoint to initialize a chat with a video transcript.
     * URL: POST http://localhost:8080/ai/init/
     */
//    @PostMapping("/chat/{meetingId}/init")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public ResponseEntity<String> initChat(
//
//            @PathVariable UUID meetingId,
//            @RequestHeader("X-User-Id") String userId){
//        MeetingTranscript meeting = meetingService.findById(meetingId);
//
//        String conversationId = chatService.initializeChat(
//                UUID.fromString(userId),
//                meeting.getFileName(),
//                meeting.getCorrectedTranscript(),
//                meeting.getSource(),
//                meeting.getSegmentsJson()
//        );
//        return ResponseEntity.ok(conversationId);
//    }
//    /**
//     * Endpoint to ask a question.
//     * URL: POST http://localhost:8080/ai/chat/{chatId}
//     */
//    @PostMapping("/chat/{chatId}/ask")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public ResponseEntity<String> askQuestion(
//            @PathVariable UUID chatId,
//            @RequestBody ChatRequest chatRequest) {
//
//
//        String response = chatService.askAi(chatId, chatRequest.getMessage());
//        return ResponseEntity.ok(response);
//    }

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

    /**
     * Endpoint to ask a question.
     */
//    @PostMapping("/chat/{chatId}/ask")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public ResponseEntity<Map<String, String>> askQuestion(
//            @PathVariable UUID chatId,
//            @RequestBody ChatRequest chatRequest) {
//
//        // The service now handles the stateless retrieval from DB
//        String response = chatService.askAi(chatId, chatRequest.getMessage());
//
//        // Wrap in Map: {"reply": "..."}
//        return ResponseEntity.ok(Map.of("reply", response));
//    }

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
    /**
     * Endpoint to retrieve chat history.
     * URL: GET http://localhost:8080/ai/history/{chatId}
     */
//    @GetMapping("/history/{chatId}")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public ResponseEntity<List<Message>> getHistory(@PathVariable UUID chatId) {
//        return ResponseEntity.ok(chatService.getChatHistory(chatId));
//    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<List<ChatHistoryDto>> getHistory(@RequestHeader("X-User-Id") String userId) {
        UUID userUuid = UUID.fromString(userId);
        // This will return an array of ChatHistoryDto objects
        return ResponseEntity.ok(chatService.getGroupedChatHistory(userUuid));
    }


}