package com.backend_microservices.ai_service.controller;

import com.backend_microservices.ai_service.dto.SummarizeRequest;
import com.backend_microservices.ai_service.dto.SummarizeResponse;
import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.service.MeetingService;
import com.backend_microservices.ai_service.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final MeetingService meetingService;
    private final SummaryService summaryService;

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

}