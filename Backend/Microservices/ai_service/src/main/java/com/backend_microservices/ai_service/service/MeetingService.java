package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.client.ExtractiveSummarizationClient;
import com.backend_microservices.ai_service.client.TranscriptionClient;
import com.backend_microservices.ai_service.dto.MeetingDto;
import com.backend_microservices.ai_service.dto.SummarizeResponse;
import com.backend_microservices.ai_service.dto.SummarizeResponseWithMeetingId;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final TranscriptionClient transcriptionClient;
    private final MeetingTranscriptRepository transcriptRepo;
    private final SummaryService summaryService;
    private final SummaryRepository summaryRepo;
    private final ExtractiveSummarizationClient aiClient;


    public MeetingTranscript processMeeting(MultipartFile file, UUID userId) throws Exception {
        // remember that the file can be video or text not only video!
        File fileToProcess;
        String videoPath = null;

        if (isVideo(file)) {
            // if the uploaded file is video then we need to save it permanently (as like a dummy server)
            // so it will be used if the user wanted to reconstruct it after getting summarized without getting the
            // user to upload the video multiple times, only upload once at start and the reconstruct as you like ;)
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File directory = new File(uploadDir);

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String cleanName = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String fileName = UUID.randomUUID() + "_" + cleanName;
            File savedFile = new File(uploadDir + fileName);
            file.transferTo(savedFile);     // copy
            fileToProcess = savedFile;
            videoPath = savedFile.getAbsolutePath();

        } else {
            // if the uploaded file is only textfile, we will only make a tempfile and not save it permanently
            File tempFile = File.createTempFile("upload_", getExtension(file.getOriginalFilename()));
            file.transferTo(tempFile);
            fileToProcess = tempFile;
        }

        // now wr need to send the file to the ai, wither text or video
        TranscriptionResponse response = transcriptionClient.processFile(fileToProcess);

        // after we got what we needed from the tempfile, now we need to delete it or else you will regret afterwards TwT
        if (!isVideo(file) && fileToProcess.exists()) {
            fileToProcess.delete();
        }

        // the rest of the code doesn't even need to be explained...
        MeetingTranscript meeting = MeetingTranscript.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .transcript(response.getTranscript())
                .correctedTranscript(response.getCorrectedText())
                .source(response.getSource())
                .createdAt(LocalDateTime.now())
                .videoPath(videoPath)   // either the real path or null in case of textfile
                .build();

        // used to convert between Java objects and JSON
        ObjectMapper mapper = new ObjectMapper();

        // again segments also can be null in case of textfile
        String segmentsJson = null;

        // if the user uploaded video then we need to map from
        if (response.getSegments() != null && !response.getSegments().isEmpty()) {
            // this Jackson method converts Java objects to JSON string, this process is also called serialization
            segmentsJson = mapper.writeValueAsString(response.getSegments());
        }

        meeting.setSegmentsJson(segmentsJson);

        return transcriptRepo.save(meeting); // here it saves the transcript and its segments
    }

    public SummarizeResponseWithMeetingId processMeetingThenSummarizeExtractive(MultipartFile file, UUID userId) throws Exception {

        MeetingTranscript meeting = processMeeting(file, userId);

        MeetingDto meetingDto = MeetingDto.builder()
                .transcript(meeting.getTranscript())
                .corrected_text(meeting.getCorrectedTranscript())
                .segmentsJson(meeting.getSegmentsJson())
                .source(meeting.getSource())
                .status("success")
                .build();

        SummarizeResponse summaryResponse = summaryService.summarizeTextExtractive(meetingDto);

        // now wr will do the same steps but for the keypoints instead of segments
        ObjectMapper mapper = new ObjectMapper();
        String keypointsJson = mapper.writeValueAsString(summaryResponse.getKeypoints());

        Summary summary = Summary.builder()
                .id(UUID.randomUUID())
                .summaryJson(keypointsJson)
                .language(summaryResponse.getLanguage())
                .createdAt(LocalDateTime.now())
                .meeting(meeting)
                .title("Untitled")
                .build();

        summaryRepo.save(summary);

        SummarizeResponseWithMeetingId response = SummarizeResponseWithMeetingId.builder()
                .meeting_id(meeting.getId())
                .json_type(summaryResponse.getJson_type())
                .num_keypoints(summaryResponse.getNum_keypoints())
                .num_sentences(summaryResponse.getNum_sentences())
                .language(summaryResponse.getLanguage())
                .keypoints(summaryResponse.getKeypoints())
                .build();

        return response;
   }

    public byte[] reconstructMeeting(UUID meetingId) {

        MeetingTranscript meeting = findById(meetingId);

        // as i said million times, remember that the user can upload textfiles not just video, so of,course we can't reconstruct text
        if (!"audio".equalsIgnoreCase(meeting.getSource())){
            throw new RuntimeException("Only videos are reconstructed!");
        }

        Summary summary = summaryRepo.findByMeeting_Id(meetingId);

        if (summary == null) {
            throw new RuntimeException("Summary not found");
        }

        // now finally we use the path we made before to get the video saved in our dummy server
        File videoFile = new File(meeting.getVideoPath());

        if (!videoFile.exists()) {
            throw new RuntimeException("Video file not found");
        }

        return aiClient.reconstructVideo(videoFile, summary.getSummaryJson());
    }

    private boolean isVideo(MultipartFile file) {
        String type = file.getContentType();
        if (type != null && type.startsWith("video/")) return true;

        String name = file.getOriginalFilename();
        if (name == null) return false;

        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        return List.of("mp4", "avi", "mov", "mkv", "webm").contains(ext);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".tmp";
        return filename.substring(filename.lastIndexOf("."));
    }

    public MeetingTranscript findById(UUID meetingId) {
        return transcriptRepo.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
    }
}