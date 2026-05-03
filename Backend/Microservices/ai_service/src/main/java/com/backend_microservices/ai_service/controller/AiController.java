package com.backend_microservices.ai_service.controller;

import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Message;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.service.ChatService;
import com.backend_microservices.ai_service.service.MeetingService;
import com.backend_microservices.ai_service.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows your Frontend to call this API without CORS errors
public class AiController {

    private final MeetingService meetingService;
    private final SummaryService summaryService;
    private final ChatService chatService;


//    @PostMapping("/upload")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public MeetingTranscript uploadMeeting(
//            @RequestParam("file") MultipartFile file,
//            @RequestHeader("X-User-Id") String userId
//    ) throws Exception {
//
//        return meetingService.processMeeting(
//                file,
//                UUID.fromString(userId)
//        );
//    }
//
//    @PostMapping("/summarize")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public SummarizeResponse summarize(
//            @RequestBody SummarizeRequest request){
//
//        return summaryService
//                .summarizeText(request.getTranscript());
//    }


    @PostMapping("/upload-summarize")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<UploadResponse> uploadThenSummarize(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId
    ) throws Exception {
        MeetingTranscript meeting = meetingService.processMeeting(file, UUID.fromString(userId));
        SummarizeResponse summaryResponse = summaryService.summarizeText(meeting.getCorrectedTranscript(),meeting);
        //SummarizeResponse summary = meetingService.processMeetingThenSummarize(meeting);


        return ResponseEntity.ok(new UploadResponse(
                summaryResponse,
                meeting.getId()
        ));
    }


    /**
     * Endpoint to initialize a chat with a video transcript.
     * URL: POST http://localhost:8080/ai/init/
     */
    @PostMapping("/chat/{meetingId}/init")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<String> initChat(

            @PathVariable UUID meetingId,
            @RequestHeader("X-User-Id") String userId){
        MeetingTranscript meeting = meetingService.findById(meetingId);

        String conversationId = chatService.initializeChat(
                UUID.fromString(userId),
                meeting.getFileName(),
                meeting.getCorrectedTranscript(),
                meeting.getSource(),
                meeting.getSegmentsJson()
        );
        return ResponseEntity.ok(conversationId);
    }
    /**
     * Endpoint to ask a question.
     * URL: POST http://localhost:8080/ai/chat/{chatId}
     */
    @PostMapping("/chat/{chatId}/ask")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<String> askQuestion(
            @PathVariable UUID chatId,
            @RequestBody ChatRequest chatRequest) {


        String response = chatService.askAi(chatId, chatRequest.getMessage());
        return ResponseEntity.ok(response);
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