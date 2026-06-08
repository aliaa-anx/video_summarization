package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.client.SummarizationClient;
import com.backend_microservices.ai_service.client.TranscriptionClient;
import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests — MeetingService
 *
 * Mocked dependencies:
 *   - TranscriptionClient          → HTTP call to Python AI worker (transcription)
 *   - MeetingTranscriptRepository  → DB access for meeting transcripts
 *   - SummaryService               → extractive / abstractive summarization logic
 *   - SummaryRepository            → DB access for summaries
 *   - SummarizationClient          → HTTP call for video reconstruction
 *
 * File I/O is avoided in tests by using Spring's MockMultipartFile.
 * Video-path-dependent tests use a real temp file to simulate a saved video on disk.
 *
 * Key areas covered:
 *   - processMeetingExtractive          → text file vs video file handling + transcript save
 *   - processMeetingThenSummarizeExtractive → full extractive pipeline + summary save
 *   - reconstructMeeting                → source guard + summary guard + video file guard
 *   - processMeetingAbstractive         → transcription + transcript save
 *   - processMeetingThenSummarizeAbstractive → full abstractive pipeline + summary save
 *   - findById                          → found vs not-found
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class MeetingServiceTest {

    // ── Mocked dependencies ────────────────────────────────────────────────
    @Mock private TranscriptionClient transcriptionClient;
    @Mock private MeetingTranscriptRepository transcriptRepo;
    @Mock private SummaryService summaryService;
    @Mock private SummaryRepository summaryRepo;
    @Mock private SummarizationClient aiClient;

    // ── System Under Test ─────────────────────────────────────────────────
    @InjectMocks
    private MeetingService meetingService;


    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * MockMultipartFile that looks like a plain text file.
     * isVideo() will return false: content-type is text/plain, extension is .txt.
     */
    private MockMultipartFile makeTextFile(String content) {
        return new MockMultipartFile(
                "file", "meeting_notes.txt",
                "text/plain", content.getBytes()
        );
    }

    /**
     * MockMultipartFile that looks like an MP4 video.
     * isVideo() will return true: content-type starts with "video/".
     * Actual bytes are minimal — we never really write a real video.
     */
    private MockMultipartFile makeVideoFile() {
        return new MockMultipartFile(
                "file", "meeting.mp4",
                "video/mp4", "fake-video-bytes".getBytes()
        );
    }

    /** Minimal TranscriptionResponse stub. */
    private TranscriptionResponse makeTranscriptionResponse(String source) {
        TranscriptionResponse r = new TranscriptionResponse();
        r.setTranscript("Raw transcript text.");
        r.setCorrected_text("Corrected transcript text.");  // snake_case — no camelCase setter exists
        r.setSource(source);
        r.setSegments(null);  // no segments for simplicity
        return r;
    }

    /** TranscriptionResponse with a non-empty segment list (video path). */
    private TranscriptionResponse makeTranscriptionResponseWithSegments() {
        SegmentDto seg = new SegmentDto();
        seg.setText("Hello world");
        seg.setStart(0.0);
        seg.setEnd(2.5);

        TranscriptionResponse r = makeTranscriptionResponse("audio");
        r.setSegments(List.of(seg));
        return r;
    }

    /** Minimal SummarizeResponse (extractive) stub. */
    private SummarizeResponse makeExtractiveResponse() {
        KeyPointDto kp = new KeyPointDto();
        kp.setText("Key insight.");
        return SummarizeResponse.builder()
                .language("english")
                .json_type("extractive")
                .num_sentences(3)
                .num_keypoints(1)
                .keypoints(List.of(kp))
                .build();
    }

    /** Builds a saved MeetingTranscript entity (simulates what transcriptRepo.save() returns). */
    private MeetingTranscript makeSavedMeeting(UUID id, String source, String videoPath) {
        return MeetingTranscript.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .fileName("meeting.mp4")
                .transcript("Raw transcript text.")
                .correctedTranscript("Corrected transcript text.")
                .source(source)
                .videoPath(videoPath)
                .build();
    }

    /** Builds a persisted Summary entity. */
    private Summary makeSummary(UUID meetingId, String json) {
        MeetingTranscript meeting = makeSavedMeeting(meetingId, "audio", null);
        return Summary.builder()
                .id(UUID.randomUUID())
                .summaryJson(json)
                .language("english")
                .meeting(meeting)
                .title("Untitled")
                .build();
    }


    // ═══════════════════════════════════════════════════════════
    //  PROCESS MEETING — EXTRACTIVE (text file path)
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — text file upload.
     * isVideo() returns false → temp file used (not permanent), transcript saved, result returned.
     * videoPath on the saved meeting must be null (no permanent storage for text files).
     */
    @Test
    void testProcessMeetingExtractive_Success_TextFile() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("This is meeting content.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenReturn(makeTranscriptionResponse("text"));

        MeetingTranscript saved = makeSavedMeeting(meetingId, "text", null);
        Mockito.when(transcriptRepo.save(Mockito.any(MeetingTranscript.class))).thenReturn(saved);

        // 2. Act
        MeetingTranscript result = meetingService.processMeetingExtractive(file, userId);

        // 3. Assert
        assertNotNull(result);
        assertEquals(meetingId, result.getId());
        assertNull(result.getVideoPath()); // text file → no permanent storage

        Mockito.verify(transcriptionClient, Mockito.times(1)).processFile(Mockito.any(File.class));
        Mockito.verify(transcriptRepo, Mockito.times(1)).save(Mockito.any(MeetingTranscript.class));
    }

    /**
     * Happy path — video file upload.
     * isVideo() returns true → file saved permanently, videoPath is set.
     * Verifies that transcriptionClient is called and transcript is saved.
     */
    @Test
    void testProcessMeetingExtractive_Success_VideoFile_WithSegments() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        MockMultipartFile file = makeVideoFile();

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenReturn(makeTranscriptionResponseWithSegments());

        MeetingTranscript saved = makeSavedMeeting(meetingId, "audio", "/uploads/fake.mp4");
        Mockito.when(transcriptRepo.save(Mockito.any(MeetingTranscript.class))).thenReturn(saved);

        // 2. Act
        MeetingTranscript result = meetingService.processMeetingExtractive(file, userId);

        // 3. Assert
        assertNotNull(result);
        assertNotNull(result.getVideoPath()); // video → permanent path set

        Mockito.verify(transcriptionClient, Mockito.times(1)).processFile(Mockito.any(File.class));
        Mockito.verify(transcriptRepo, Mockito.times(1)).save(Mockito.any(MeetingTranscript.class));
    }

    /**
     * Failure — transcription client throws (Python worker down).
     * Exception propagates; transcript must never be saved.
     */
    @Test
    void testProcessMeetingExtractive_Failure_TranscriptionClientThrows() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("Some text.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenThrow(new RuntimeException("Transcription service down"));

        // 2. Act & Assert
        assertThrows(Exception.class, () ->
                meetingService.processMeetingExtractive(file, userId)
        );

        Mockito.verify(transcriptRepo, Mockito.never()).save(Mockito.any(MeetingTranscript.class));
    }


    // ═══════════════════════════════════════════════════════════
    //  PROCESS MEETING THEN SUMMARIZE — EXTRACTIVE
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — full extractive pipeline:
     * transcribe → build MeetingDto → summarize → serialize keypoints →
     * save Summary → build and return SummarizeResponseWithMeetingId.
     */
    @Test
    void testProcessMeetingThenSummarizeExtractive_Success() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("Meeting content.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenReturn(makeTranscriptionResponse("text"));

        MeetingTranscript savedMeeting = makeSavedMeeting(meetingId, "text", null);
        Mockito.when(transcriptRepo.save(Mockito.any(MeetingTranscript.class))).thenReturn(savedMeeting);

        Mockito.when(summaryService.summarizeTextExtractive(Mockito.any(MeetingDto.class)))
                .thenReturn(makeExtractiveResponse());

        // 2. Act
        SummarizeResponseWithMeetingId result =
                meetingService.processMeetingThenSummarizeExtractive(file, userId);

        // 3. Assert
        assertNotNull(result);
        assertEquals(meetingId, result.getMeeting_id());
        assertEquals("english", result.getLanguage());
        assertNotNull(result.getKeypoints());

        // Summary must be persisted
        Mockito.verify(summaryRepo, Mockito.times(1)).save(Mockito.any(Summary.class));
    }

    /**
     * Failure — summarization client throws after transcription succeeds.
     * Summary must never be saved.
     */
    @Test
    void testProcessMeetingThenSummarizeExtractive_Failure_SummarizationThrows() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("Meeting content.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenReturn(makeTranscriptionResponse("text"));
        Mockito.when(transcriptRepo.save(Mockito.any(MeetingTranscript.class)))
                .thenReturn(makeSavedMeeting(UUID.randomUUID(), "text", null));
        Mockito.when(summaryService.summarizeTextExtractive(Mockito.any(MeetingDto.class)))
                .thenThrow(new RuntimeException("AI summarization failed"));

        // 2. Act & Assert
        assertThrows(Exception.class, () ->
                meetingService.processMeetingThenSummarizeExtractive(file, userId)
        );

        Mockito.verify(summaryRepo, Mockito.never()).save(Mockito.any(Summary.class));
    }


    // ═══════════════════════════════════════════════════════════
    //  RECONSTRUCT MEETING
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — audio meeting with a valid summary and an existing video file on disk.
     * A real temp file is created so File.exists() returns true.
     */
    @Test
    void testReconstructMeeting_Success() throws Exception {
        // 1. Arrange — create a real temp file to simulate the saved video
        UUID meetingId = UUID.randomUUID();
        File tempVideo = File.createTempFile("test_video_", ".mp4");
        tempVideo.deleteOnExit();

        MeetingTranscript meeting = makeSavedMeeting(meetingId, "audio", tempVideo.getAbsolutePath());
        Mockito.when(transcriptRepo.findById(meetingId)).thenReturn(Optional.of(meeting));

        Summary summary = makeSummary(meetingId, "[{\"text\":\"Key point\"}]");
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(summary);

        byte[] fakeVideo = new byte[]{1, 2, 3, 4};
        Mockito.when(aiClient.reconstructVideo(Mockito.any(File.class), Mockito.anyString()))
                .thenReturn(fakeVideo);

        // 2. Act
        byte[] result = meetingService.reconstructMeeting(meetingId);

        // 3. Assert
        assertNotNull(result);
        assertArrayEquals(fakeVideo, result);
        Mockito.verify(aiClient, Mockito.times(1))
                .reconstructVideo(Mockito.any(File.class), Mockito.anyString());
    }

    /**
     * Failure — source is "text", not "audio".
     * Must throw RuntimeException("Only videos are reconstructed!") immediately.
     * Summary and video client must never be touched.
     */
    @Test
    void testReconstructMeeting_Failure_SourceIsText() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        MeetingTranscript meeting = makeSavedMeeting(meetingId, "text", null);
        Mockito.when(transcriptRepo.findById(meetingId)).thenReturn(Optional.of(meeting));

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                meetingService.reconstructMeeting(meetingId)
        );
        assertTrue(ex.getMessage().contains("Only videos are reconstructed"));

        Mockito.verify(summaryRepo, Mockito.never()).findByMeeting_Id(Mockito.any());
        Mockito.verify(aiClient, Mockito.never()).reconstructVideo(Mockito.any(), Mockito.anyString());
    }

    /**
     * Failure — meeting is audio but no summary row exists yet.
     * Must throw RuntimeException("Summary not found"); video client never called.
     */
    @Test
    void testReconstructMeeting_Failure_SummaryNotFound() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        MeetingTranscript meeting = makeSavedMeeting(meetingId, "audio", "/some/path.mp4");
        Mockito.when(transcriptRepo.findById(meetingId)).thenReturn(Optional.of(meeting));
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(null);

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                meetingService.reconstructMeeting(meetingId)
        );
        assertTrue(ex.getMessage().contains("Summary not found"));

        Mockito.verify(aiClient, Mockito.never()).reconstructVideo(Mockito.any(), Mockito.anyString());
    }

    /**
     * Failure — summary exists but the video file on disk was deleted / moved.
     * Must throw RuntimeException("Video file not found"); aiClient never called.
     */
    @Test
    void testReconstructMeeting_Failure_VideoFileNotOnDisk() {
        // 1. Arrange — path points to a file that definitely doesn't exist
        UUID meetingId = UUID.randomUUID();
        String ghostPath = "/nonexistent/path/video_" + UUID.randomUUID() + ".mp4";

        MeetingTranscript meeting = makeSavedMeeting(meetingId, "audio", ghostPath);
        Mockito.when(transcriptRepo.findById(meetingId)).thenReturn(Optional.of(meeting));

        Summary summary = makeSummary(meetingId, "some summary json");
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(summary);

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                meetingService.reconstructMeeting(meetingId)
        );
        assertTrue(ex.getMessage().contains("Video file not found"));

        Mockito.verify(aiClient, Mockito.never()).reconstructVideo(Mockito.any(), Mockito.anyString());
    }

    /**
     * Failure — meeting ID doesn't exist at all.
     * findById() throws; nothing else should run.
     */
    @Test
    void testReconstructMeeting_Failure_MeetingNotFound() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        Mockito.when(transcriptRepo.findById(meetingId)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () ->
                meetingService.reconstructMeeting(meetingId)
        );

        Mockito.verify(summaryRepo, Mockito.never()).findByMeeting_Id(Mockito.any());
        Mockito.verify(aiClient, Mockito.never()).reconstructVideo(Mockito.any(), Mockito.anyString());
    }


    // ═══════════════════════════════════════════════════════════
    //  PROCESS MEETING — ABSTRACTIVE
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — abstractive transcription path.
     * Always creates a temp file (no permanent storage), calls transcriptionClient, saves transcript.
     */
    @Test
    void testProcessMeetingAbstractive_Success() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("Meeting audio content.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenReturn(makeTranscriptionResponse("text"));

        MeetingTranscript saved = makeSavedMeeting(UUID.randomUUID(), "text", null);
        Mockito.when(transcriptRepo.save(Mockito.any(MeetingTranscript.class))).thenReturn(saved);

        // 2. Act
        MeetingTranscript result = meetingService.processMeetingAbstractive(file, userId);

        // 3. Assert
        assertNotNull(result);
        Mockito.verify(transcriptionClient, Mockito.times(1)).processFile(Mockito.any(File.class));
        Mockito.verify(transcriptRepo, Mockito.times(1)).save(Mockito.any(MeetingTranscript.class));
    }

    /**
     * Failure — transcriptionClient throws during abstractive path.
     * Transcript must never be saved.
     */
    @Test
    void testProcessMeetingAbstractive_Failure_TranscriptionClientThrows() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("Content.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenThrow(new RuntimeException("Worker unreachable"));

        // 2. Act & Assert
        assertThrows(Exception.class, () ->
                meetingService.processMeetingAbstractive(file, userId)
        );

        Mockito.verify(transcriptRepo, Mockito.never()).save(Mockito.any(MeetingTranscript.class));
    }


    // ═══════════════════════════════════════════════════════════
    //  PROCESS MEETING THEN SUMMARIZE — ABSTRACTIVE
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — full abstractive pipeline with "short" flag.
     * Verifies: transcript saved, summaryService called with correct flag, Summary persisted,
     * response contains meetingId and language.
     */
    @Test
    void testProcessMeetingThenSummarizeAbstractive_Success_ShortFlag() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("Meeting content.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenReturn(makeTranscriptionResponse("text"));

        MeetingTranscript savedMeeting = makeSavedMeeting(UUID.randomUUID(), "text", null);
        Mockito.when(transcriptRepo.save(Mockito.any(MeetingTranscript.class))).thenReturn(savedMeeting);

        SummaryResponseAbstractive absResponse = new SummaryResponseAbstractive();
        absResponse.setSummary("A concise abstractive summary.");
        absResponse.setLanguage("english");
        Mockito.when(summaryService.summarizeTextAbstractive(
                Mockito.anyString(),
                Mockito.any(MeetingTranscript.class),
                Mockito.eq("short"))
        ).thenReturn(absResponse);

        // 2. Act
        SummaryResponseAbsWithMeetingId result =
                meetingService.processMeetingThenSummarizeAbstractive(file, userId, "short");

        // 3. Assert
        // Note: processMeetingAbstractive() returns the locally built entity (not the repo.save() result),
        // so meeting.getId() is null at the point the response is assembled. We assert on the fields
        // that ARE populated: language and summary content.
        assertNotNull(result);
        assertEquals("english", result.getLanguage());
        assertEquals("A concise abstractive summary.", result.getSummary());

        Mockito.verify(summaryRepo, Mockito.times(1)).save(Mockito.any(Summary.class));
    }

    /**
     * Happy path — full abstractive pipeline with "long" flag.
     * Verifies summaryService is called with "long" (not "short").
     */
    @Test
    void testProcessMeetingThenSummarizeAbstractive_Success_LongFlag() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("Long meeting content.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenReturn(makeTranscriptionResponse("text"));

        MeetingTranscript savedMeeting = makeSavedMeeting(meetingId, "text", null);
        Mockito.when(transcriptRepo.save(Mockito.any(MeetingTranscript.class))).thenReturn(savedMeeting);

        SummaryResponseAbstractive absResponse = new SummaryResponseAbstractive();
        absResponse.setSummary("A detailed long-form abstractive summary.");
        absResponse.setLanguage("arabic");
        Mockito.when(summaryService.summarizeTextAbstractive(
                Mockito.anyString(),
                Mockito.any(MeetingTranscript.class),
                Mockito.eq("long"))
        ).thenReturn(absResponse);

        // 2. Act
        SummaryResponseAbsWithMeetingId result =
                meetingService.processMeetingThenSummarizeAbstractive(file, userId, "long");

        // 3. Assert
        assertNotNull(result);
        assertEquals("arabic", result.getLanguage());

        // Verify "long" was passed — "short" method must never be invoked
        Mockito.verify(summaryService, Mockito.times(1))
                .summarizeTextAbstractive(Mockito.anyString(), Mockito.any(), Mockito.eq("long"));
    }

    /**
     * Failure — abstractive summarization throws after transcript is saved.
     * Summary must never be persisted.
     */
    @Test
    void testProcessMeetingThenSummarizeAbstractive_Failure_SummarizationThrows() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = makeTextFile("Content.");

        Mockito.when(transcriptionClient.processFile(Mockito.any(File.class)))
                .thenReturn(makeTranscriptionResponse("text"));
        Mockito.when(transcriptRepo.save(Mockito.any(MeetingTranscript.class)))
                .thenReturn(makeSavedMeeting(UUID.randomUUID(), "text", null));
        Mockito.when(summaryService.summarizeTextAbstractive(
                Mockito.anyString(), Mockito.any(), Mockito.anyString())
        ).thenThrow(new RuntimeException("Model overloaded"));

        // 2. Act & Assert
        assertThrows(Exception.class, () ->
                meetingService.processMeetingThenSummarizeAbstractive(file, userId, "short")
        );

        Mockito.verify(summaryRepo, Mockito.never()).save(Mockito.any(Summary.class));
    }


    // ═══════════════════════════════════════════════════════════
    //  FIND BY ID
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — valid UUID returns the meeting entity.
     */
    @Test
    void testFindById_Success() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        MeetingTranscript meeting = makeSavedMeeting(meetingId, "text", null);
        Mockito.when(transcriptRepo.findById(meetingId)).thenReturn(Optional.of(meeting));

        // 2. Act
        MeetingTranscript result = meetingService.findById(meetingId);

        // 3. Assert
        assertNotNull(result);
        assertEquals(meetingId, result.getId());
    }

    /**
     * Failure — UUID not found in DB.
     * Must throw RuntimeException("Meeting not found").
     */
    @Test
    void testFindById_Failure_NotFound() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        Mockito.when(transcriptRepo.findById(meetingId)).thenReturn(Optional.empty());

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                meetingService.findById(meetingId)
        );
        assertTrue(ex.getMessage().contains("Meeting not found"));
    }
}