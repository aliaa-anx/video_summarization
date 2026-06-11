package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.client.NotificationClient;
import com.backend_microservices.ai_service.client.SummarizationClient;
import com.backend_microservices.ai_service.client.UserClient;
import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests — SummaryService
 *
 * Mocked dependencies:
 *   - SummarizationClient  → HTTP calls to the Python AI worker (extractive, abstractive, TTS)
 *   - SummaryRepository    → DB access for persisted summaries
 *
 * No real DB, no real network calls — everything is controlled via Mockito.
 *
 * Key areas covered:
 *   - summarizeTextExtractive  → segment parsing + request building + client delegation
 *   - summarizeTextAbstractive → long/short flag routing to correct client method
 *   - convertSummaryToAudio    → lookup → language → getText → TTS call
 *   - getText (via audio path) → plain text vs JSON segment extraction + timestamp sorting
 *   - extractTitle             → ## marker parsing + edge cases
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class SummaryServiceTest {

    // ── Mocked dependencies ────────────────────────────────────────────────
    @Mock private SummarizationClient aiClient;
    @Mock private SummaryRepository summaryRepo;
    // Add these two mocks at the top with the others
    @Mock private NotificationClient notificationClient;
    @Mock private UserClient userClient;

    // ── System Under Test ─────────────────────────────────────────────────
    @InjectMocks
    private SummaryService summaryService;


    // ── Helpers ───────────────────────────────────────────────────────────

    /** Builds a MeetingDto for text-based uploads (no segments JSON). */
    private MeetingDto makeTextMeetingDto(String transcript) {
        MeetingDto dto = new MeetingDto();
        dto.setTranscript(transcript);
        dto.setSource("text");
        dto.setCorrected_text(transcript);
        dto.setSegmentsJson(null);   // text upload → no segments
        return dto;
    }

    /** Builds a MeetingDto for video/audio uploads with a segments JSON payload. */
    private MeetingDto makeVideoMeetingDto(String transcript, String segmentsJson) {
        MeetingDto dto = new MeetingDto();
        dto.setTranscript(transcript);
        dto.setSource("video");
        dto.setCorrected_text(transcript);
        dto.setSegmentsJson(segmentsJson);
        return dto;
    }

    /** Minimal valid segments JSON with timestamps. */
    private String validSegmentsJson() {
        return "[{\"text\":\"Hello world\",\"start\":0.0,\"end\":2.5}," +
                " {\"text\":\"How are you\",\"start\":3.0,\"end\":5.0}]";
    }

    /** Builds a persisted Summary entity for the audio conversion tests. */
    private Summary makeSummary(UUID meetingId, String summaryJson, String language) {
        Summary s = new Summary();
        s.setSummaryJson(summaryJson);
        s.setLanguage(language);
        // attach a minimal Meeting stub so getLanguage() works
        com.backend_microservices.ai_service.entity.MeetingTranscript meeting =
                new com.backend_microservices.ai_service.entity.MeetingTranscript();
        meeting.setId(meetingId);
        s.setMeeting(meeting);
        return s;
    }

    /**
     * Builds a minimal SummarizeResponse using the @Builder from Lombok.
     * SummarizeResponse has: language, json_type, num_sentences, num_keypoints, keypoints.
     * Tests that only care "client was called and returned non-null" use this.
     */
    private SummarizeResponse makeSummarizeResponse() {
        KeyPointDto kp = new KeyPointDto();
        kp.setText("Key point one.");
        return SummarizeResponse.builder()
                .language("english")
                .json_type("extractive")
                .num_sentences(3)
                .num_keypoints(1)
                .keypoints(List.of(kp))
                .build();
    }



    // ═══════════════════════════════════════════════════════════
    //  SUMMARIZE TEXT — EXTRACTIVE
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — text upload (no segments JSON).
     * segmentsJson is null → segments list defaults to empty → client still called once.
     */
    @Test
    void testSummarizeTextExtractive_Success_TextMode_NullSegments() throws JsonProcessingException {
        // 1. Arrange
        MeetingDto dto = makeTextMeetingDto("This is the full transcript of the meeting.");
        SummarizeResponse expected = makeSummarizeResponse();

        Mockito.when(aiClient.summarizeExtractive(Mockito.any(SummarizeRequest.class)))
                .thenReturn(expected);

        // 2. Act
        SummarizeResponse result = summaryService.summarizeTextExtractive(dto);

        // 3. Assert — response is the exact object the client returned
        assertNotNull(result);
        assertEquals("english", result.getLanguage());
        assertEquals(1, result.getKeypoints().size());
        Mockito.verify(aiClient, Mockito.times(1))
                .summarizeExtractive(Mockito.any(SummarizeRequest.class));
    }

    /**
     * Happy path — video/audio upload with a valid segments JSON payload.
     * Segments are parsed correctly and forwarded to the AI client.
     */
    @Test
    void testSummarizeTextExtractive_Success_VideoMode_WithSegments() throws JsonProcessingException {
        // 1. Arrange
        MeetingDto dto = makeVideoMeetingDto("Full transcript here.", validSegmentsJson());
        SummarizeResponse expected = makeSummarizeResponse();

        Mockito.when(aiClient.summarizeExtractive(Mockito.any(SummarizeRequest.class)))
                .thenReturn(expected);

        // 2. Act
        SummarizeResponse result = summaryService.summarizeTextExtractive(dto);

        // 3. Assert — segments were parsed and client was reached
        assertNotNull(result);
        assertNotNull(result.getKeypoints());
        Mockito.verify(aiClient, Mockito.times(1))
                .summarizeExtractive(Mockito.any(SummarizeRequest.class));
    }

    /**
     * Edge case — segmentsJson is a blank string (not null, but whitespace-only).
     * The filter(!s.isBlank()) guard must treat it as absent → empty segments list,
     * no JSON parse attempted, no exception thrown.
     */
    @Test
    void testSummarizeTextExtractive_Success_BlankSegmentsJson_DefaultsToEmpty() throws JsonProcessingException {
        // 1. Arrange
        MeetingDto dto = makeVideoMeetingDto("Transcript.", "   "); // blank, not null
        Mockito.when(aiClient.summarizeExtractive(Mockito.any(SummarizeRequest.class)))
                .thenReturn(makeSummarizeResponse());

        // 2. Act & Assert — must not throw
        assertDoesNotThrow(() -> summaryService.summarizeTextExtractive(dto));

        Mockito.verify(aiClient, Mockito.times(1))
                .summarizeExtractive(Mockito.any(SummarizeRequest.class));
    }

    /**
     * Failure — segmentsJson contains malformed JSON (not a valid list).
     * The mapper.readValue() call inside the lambda must propagate a RuntimeException.
     */
    @Test
    void testSummarizeTextExtractive_Failure_MalformedSegmentsJson() {
        // 1. Arrange
        MeetingDto dto = makeVideoMeetingDto("Transcript.", "{not valid json list}");

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () ->
                summaryService.summarizeTextExtractive(dto)
        );

        // Client must never be reached if parsing explodes
        Mockito.verify(aiClient, Mockito.never())
                .summarizeExtractive(Mockito.any(SummarizeRequest.class));
    }

    /**
     * Failure — AI client throws (Python worker down, network error, etc.).
     * Exception must propagate to the caller.
     */
    @Test
    void testSummarizeTextExtractive_Failure_ClientThrows() {
        // 1. Arrange
        MeetingDto dto = makeTextMeetingDto("Any transcript.");
        Mockito.when(aiClient.summarizeExtractive(Mockito.any(SummarizeRequest.class)))
                .thenThrow(new RuntimeException("Python service unavailable"));

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () ->
                summaryService.summarizeTextExtractive(dto)
        );
    }


    // ═══════════════════════════════════════════════════════════
    //  SUMMARIZE TEXT — ABSTRACTIVE
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — "long" flag routes to summarizeAbstractiveLong().
     * summarizeAbstractiveShort() must never be called.
     */
    @Test
    void testSummarizeTextAbstractive_Success_LongFlag_CallsLongMethod() {
        // 1. Arrange
        String transcript = "A very long meeting transcript...";
        MeetingTranscript meeting = new MeetingTranscript();
        SummaryResponseAbstractive expected = new SummaryResponseAbstractive();

        Mockito.when(aiClient.summarizeAbstractiveLong(transcript)).thenReturn(expected);

        // 2. Act
        SummaryResponseAbstractive result =
                summaryService.summarizeTextAbstractive(transcript, meeting, "long");

        // 3. Assert — correct client method called, result is exactly what the client returned
        assertNotNull(result);
        assertSame(expected, result);
        Mockito.verify(aiClient, Mockito.times(1)).summarizeAbstractiveLong(transcript);
        Mockito.verify(aiClient, Mockito.never()).summarizeAbstractiveShort(Mockito.anyString());
    }

    /**
     * Happy path — any flag other than "long" routes to summarizeAbstractiveShort().
     * summarizeAbstractiveLong() must never be called.
     */
    @Test
    void testSummarizeTextAbstractive_Success_ShortFlag_CallsShortMethod() {
        // 1. Arrange
        String transcript = "Short meeting.";
        MeetingTranscript meeting = new MeetingTranscript();
        SummaryResponseAbstractive expected = new SummaryResponseAbstractive();

        Mockito.when(aiClient.summarizeAbstractiveShort(transcript)).thenReturn(expected);

        // 2. Act
        SummaryResponseAbstractive result =
                summaryService.summarizeTextAbstractive(transcript, meeting, "short");

        // 3. Assert
        assertNotNull(result);
        assertSame(expected, result);
        Mockito.verify(aiClient, Mockito.times(1)).summarizeAbstractiveShort(transcript);
        Mockito.verify(aiClient, Mockito.never()).summarizeAbstractiveLong(Mockito.anyString());
    }

    /**
     * Failure — AI client throws during abstractive summarization.
     * Exception must propagate to the caller regardless of flag.
     */
    @Test
    void testSummarizeTextAbstractive_Failure_ClientThrows() {
        // 1. Arrange
        Mockito.when(aiClient.summarizeAbstractiveShort(Mockito.anyString()))
                .thenThrow(new RuntimeException("Model timeout"));

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () ->
                summaryService.summarizeTextAbstractive("transcript", new MeetingTranscript(), "short")
        );
    }


    // ═══════════════════════════════════════════════════════════
    //  CONVERT SUMMARY TO AUDIO
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — English summary (plain text, not JSON).
     * Verifies: correct speaker selected, audio bytes returned, client called once.
     */
    @Test
    void testConvertSummaryToAudio_Success_EnglishPlainText() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        Summary summary = makeSummary(meetingId, "Plain text summary.", "english");
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(summary);

        byte[] fakeAudio = new byte[]{1, 2, 3};
        Mockito.when(aiClient.generateAudio(Mockito.eq("Plain text summary."),
                Mockito.eq("en"))).thenReturn(fakeAudio);

        // 2. Act
        byte[] result = summaryService.convertSummaryToAudio(meetingId,false,UUID.randomUUID());

        // 3. Assert
        assertNotNull(result);
        assertArrayEquals(fakeAudio, result);
        Mockito.verify(aiClient, Mockito.times(1))
                .generateAudio("Plain text summary.", "en");
    }

    /**
     * Happy path — Arabic summary (plain text).
     * Verifies the Arabic speaker "ar-EG-ShakirNeural" is selected for non-English.
     */
//    @Test
//    void testConvertSummaryToAudio_Success_ArabicPlainText() {
//        // 1. Arrange
//        UUID meetingId = UUID.randomUUID();
//        Summary summary = makeSummary(meetingId, "ملخص النص.", "arabic");
//        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(summary);
//
//        byte[] fakeAudio = new byte[]{9, 8, 7};
//        Mockito.when(aiClient.generateAudio(Mockito.eq("ملخص النص."),
//                Mockito.eq("ar-EG-ShakirNeural"))).thenReturn(fakeAudio);
//
//        // 2. Act
//        byte[] result = summaryService.convertSummaryToAudio(meetingId,false,UUID.randomUUID());
//
//        // 3. Assert
//        assertArrayEquals(fakeAudio, result);
//        Mockito.verify(aiClient, Mockito.times(1))
//                .generateAudio("ملخص النص.", "ar-EG-ShakirNeural");
//    }
    @Test
    void testConvertSummaryToAudio_Success_ArabicPlainText() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        Summary summary = makeSummary(meetingId, "ملخص النص.", "arabic");
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(summary);

        byte[] fakeAudio = new byte[]{9, 8, 7};
        Mockito.when(aiClient.generateAudio(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(fakeAudio);

        // 2. Act
        byte[] result = summaryService.convertSummaryToAudio(meetingId, false, UUID.randomUUID());

        // 3. Assert — audio bytes returned, and Arabic speaker "ar" was used
        assertArrayEquals(fakeAudio, result);
        Mockito.verify(aiClient, Mockito.times(1))
                .generateAudio(Mockito.eq("ملخص النص."), Mockito.eq("ar"));
    }

    /**
     * Happy path — English summary stored as a JSON segments array (extractive output).
     * getText() must parse the array, sort by timestamps, join with ", ", and pass the
     * resulting plain string to generateAudio().
     */
    @Test
    void testConvertSummaryToAudio_Success_EnglishJsonSegments() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        String segmentsJson = "[{\"text\":\"First point.\",\"start\":0.0,\"end\":2.0}," +
                "{\"text\":\"Second point.\",\"start\":3.0,\"end\":5.0}]";
        Summary summary = makeSummary(meetingId, segmentsJson, "english");
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(summary);

        byte[] fakeAudio = new byte[]{4, 5, 6};
        // getText() joins with ", " after sorting by start time
        String expectedText = "First point., Second point.";
        Mockito.when(aiClient.generateAudio(Mockito.eq(expectedText),
                Mockito.eq("en"))).thenReturn(fakeAudio);

        // 2. Act
        byte[] result = summaryService.convertSummaryToAudio(meetingId,false,UUID.randomUUID());

        // 3. Assert
        assertNotNull(result);
        assertArrayEquals(fakeAudio, result);
    }

    /**
     * Failure — no Summary row exists for the given meetingId.
     * Must throw RuntimeException("Summary not found"); audio client never called.
     */
    @Test
    void testConvertSummaryToAudio_Failure_SummaryNotFound() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(null);

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                summaryService.convertSummaryToAudio(meetingId,false,UUID.randomUUID())
        );
        assertTrue(ex.getMessage().contains("Summary not found"));
        Mockito.verify(aiClient, Mockito.never())
                .generateAudio(Mockito.anyString(), Mockito.anyString());
    }

    /**
     * Failure — summary JSON segments are malformed.
     * getText() must throw RuntimeException("Invalid segment JSON format").
     */
    @Test
    void testConvertSummaryToAudio_Failure_MalformedSegmentsJson() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        Summary summary = makeSummary(meetingId, "[{broken json", "english");
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(summary);

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                summaryService.convertSummaryToAudio(meetingId,false,UUID.randomUUID())
        );
        assertTrue(ex.getMessage().contains("Invalid segment JSON format"));
        Mockito.verify(aiClient, Mockito.never())
                .generateAudio(Mockito.anyString(), Mockito.anyString());
    }

    /**
     * Edge case — summaryJson is null.
     * getText() returns "" for null input → generateAudio called with empty string, no crash.
     */
    @Test
    void testConvertSummaryToAudio_Success_NullSummaryJson_PassesEmptyString() {
        // 1. Arrange
        UUID meetingId = UUID.randomUUID();
        Summary summary = makeSummary(meetingId, null, "english");
        Mockito.when(summaryRepo.findByMeeting_Id(meetingId)).thenReturn(summary);
        Mockito.when(aiClient.generateAudio(Mockito.eq(""), Mockito.eq("en-US-GuyNeural")))
                .thenReturn(new byte[]{});

        // 2. Act — must not throw
        assertDoesNotThrow(() -> summaryService.convertSummaryToAudio(meetingId,false,UUID.randomUUID()));

        Mockito.verify(aiClient, Mockito.times(1))
                .generateAudio("", "en");
    }


    // ═══════════════════════════════════════════════════════════
    //  EXTRACT TITLE
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — summary contains the ## Title ## marker.
     * extractTitle() must return the trimmed text between the markers.
     */
    @Test
    void testExtractTitle_Success_ValidMarker() {
        // 1. Arrange
        String summary = "## Project Kickoff Meeting ## Some content here...";

        // 2. Act
        String title = summaryService.extractTitle(summary);

        // 3. Assert
        assertEquals("Project Kickoff Meeting", title);
    }

    /**
     * Edge case — summary has the marker with extra whitespace inside.
     * Trimming must strip leading/trailing spaces from the captured group.
     */
    @Test
    void testExtractTitle_Success_MarkerWithExtraWhitespace() {
        // 1. Arrange
        String summary = "##   Q3 Review   ## rest of summary";

        // 2. Act
        String title = summaryService.extractTitle(summary);

        // 3. Assert
        assertEquals("Q3 Review", title);
    }

    /**
     * Edge case — summary has no ## marker at all.
     * Must return "Untitled" instead of throwing.
     */
    @Test
    void testExtractTitle_Failure_NoMarker_ReturnsUntitled() {
        // 1. Arrange
        String summary = "This is a plain summary with no title marker.";

        // 2. Act
        String title = summaryService.extractTitle(summary);

        // 3. Assert
        assertEquals("Untitled", title);
    }

    /**
     * Edge case — null summary input.
     * Must return "Untitled" without throwing NullPointerException.
     */
    @Test
    void testExtractTitle_Failure_NullInput_ReturnsUntitled() {
        // 1. Act
        String title = summaryService.extractTitle(null);

        // 2. Assert
        assertEquals("Untitled", title);
    }

    /**
     * Edge case — blank (whitespace-only) summary input.
     * Must return "Untitled" without throwing.
     */
    @Test
    void testExtractTitle_Failure_BlankInput_ReturnsUntitled() {
        // 1. Act
        String title = summaryService.extractTitle("   ");

        // 2. Assert
        assertEquals("Untitled", title);
    }
}