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
    public SummarizeResponse uploadThenSummarize(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId
    ) throws Exception {

        return meetingService.processMeetingThenSummarize(
                file,
                UUID.fromString(userId)
        );
    }



    /**
     * Endpoint to initialize a chat with a video transcript.
     * URL: POST http://localhost:8080/ai/init/{userId}
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<UploadResponse> uploadMeeting(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId
    ) throws Exception {

        // 1. Process meeting — get transcript
//        MeetingTranscript meeting = meetingService.processMeeting(
//                file,
//                UUID.fromString(userId)
//        );
        //TODO: replace this with real transcription when model is available
        String mockTranscript = "This meeting discussed Q3 targets. " +
                "The team agreed to increase sales by 20 percent. " +
                "Marketing will focus on social media campaigns. " +
                "Product team will release new features by end of month.";


        // 2. Initialize chat with the transcript
        String conversationId = chatService.initializeChat(
                UUID.fromString(userId),
                file.getOriginalFilename(),
                //meeting.getCorrectedTranscript()
                mockTranscript
        );

        // 3. Return transcript + conversationId
        return ResponseEntity.ok(new UploadResponse(
                //meeting.getTranscript(),
                mockTranscript,
                conversationId
        ));
    }
    @PostMapping("/chat/init")
    public ResponseEntity<String> initChat(

            @RequestParam String title,
            @RequestBody InitRequest initRequest,
            @RequestHeader("X-User-Id") String userId){

        String conversationId = chatService.initializeChat(
                UUID.fromString(userId),
                 title,
                initRequest.getTranscript()
                );
        return ResponseEntity.ok(conversationId);
    }
    /**
     * Endpoint to ask a question.
     * URL: POST http://localhost:8080/ai/chat/{chatId}
     */
    @PostMapping("/chat/{chatId}/ask")
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
    @GetMapping("/history/{chatId}")
    public ResponseEntity<List<Message>> getHistory(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatHistory(chatId));
    }

//    @PostMapping("/upload-summarize")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public SummarizeResponse uploadThenSummarize(
//            @RequestParam("file") MultipartFile file,
//            @RequestHeader("X-User-Id") String userId
//    ) throws Exception {
//
//
////        // 1. Process meeting (get transcript directly)
////        MeetingTranscript meeting = meetingService.processMeeting(file, UUID.fromString(userId));
////
////        // 2. Summarize
////        SummarizeResponse summaryResponse = summaryService.summarizeText(meeting.getCorrectedTranscript());
////
////        // 3. Initialize chat  (returns conversationId)
////        String conversationId = chatService.initializeChat(
////                UUID.fromString(userId),
////                file.getOriginalFilename(),
////                meeting.getCorrectedTranscript()
////        );
////
////        // 4. Return everything
////        return ResponseEntity.ok(new SummarizeResponse(
////                summaryResponse.getSummary(),
////                summaryResponse.getLanguage(),
////                conversationId
////        ));
//
//
//    }


}