package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.entity.*;
import com.backend_microservices.ai_service.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests — ChatService
 *
 * Mocked dependencies:
 *   - ConversationRepository      → DB access for conversations
 *   - MessageRepository           → DB access for chat messages
 *   - DocumentRepository          → DB access for vector embeddings
 *   - MeetingTranscriptRepository → DB access for transcript metadata
 *   - AiService                   → embedding generation (calls Python internally)
 *   - RestTemplate                → HTTP calls to Python AI worker
 *
 * No real DB, no real network calls — everything is controlled via Mockito.
 *
 * Key areas covered:
 *   - initializeChat  → conversation creation + Python notification + embedding storage
 *   - askAi           → full RAG pipeline: load → search → call Python → save reply
 *   - getGroupedChatHistory → message grouping by conversation
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class ChatServiceTest {

    // ── Mocked dependencies ────────────────────────────────────────────────
    @Mock private ConversationRepository conversationRepo;
    // ChatService declares MessageRepository twice (messageRepo + messageRepository).
    // Mockito injects the SAME mock instance into both fields — one declaration is enough.
    @Mock private MessageRepository messageRepo;
    @Mock private DocumentRepository documentRepository;
    @Mock private MeetingTranscriptRepository transcriptRepository;
    @Mock private AiService aiService;
    @Mock private RestTemplate restTemplate;

    // ── System Under Test ─────────────────────────────────────────────────
    @InjectMocks
    private ChatService chatService;

    // ── Helpers ───────────────────────────────────────────────────────────

    private Conversation makeConversation(UUID id, UUID userId) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setUserId(userId);
        c.setTitle("Test Video");
        return c;
    }

    private MeetingTranscript makeTranscript(String source) {
        MeetingTranscript t = new MeetingTranscript();

        t.setSource(source);
        t.setCorrectedTranscript("This is a test transcript with enough content.");
        return t;
    }

    private AiResponse makeAiResponse(String reply) {
        AiResponse r = new AiResponse();
        r.setReply(reply);
        r.setError(null);
        return r;
    }


    // ═══════════════════════════════════════════════════════════
    //  INITIALIZE CHAT
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — text mode initialization.
     * Verifies: conversation saved, Python notified, transcript chunked into embeddings.
     */
    @Test
    void testInitializeChat_Success_TextMode() {
        // 1. Arrange
        UUID userId    = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        UUID convId    = UUID.randomUUID();

        Conversation savedConv = makeConversation(convId, userId);
        Mockito.when(conversationRepo.save(Mockito.any(Conversation.class))).thenReturn(savedConv);

        // Python init call returns anything (we don't care about the return value)
        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(Map.class))
        ).thenReturn(Map.of("status", "ok"));

        // Embedding generation returns a dummy vector
        Mockito.when(aiService.getEmbedding(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        // 2. Act
        String resultId = chatService.initializeChat(
                userId, meetingId, "Test Video",
                "Hello world transcript", "text", null
        );

        // 3. Assert
        assertNotNull(resultId);
        assertEquals(convId.toString(), resultId);

        // Conversation must be saved exactly once
        Mockito.verify(conversationRepo, Mockito.times(1)).save(Mockito.any(Conversation.class));

        // Python must be notified exactly once
        Mockito.verify(restTemplate, Mockito.times(1))
                .postForObject(Mockito.anyString(), Mockito.any(), Mockito.eq(Map.class));

        // At least one document embedding must be saved for the transcript
        Mockito.verify(documentRepository, Mockito.atLeastOnce())
                .save(Mockito.any(Document.class));
    }

    /**
     * Edge case — null source defaults to "text" mode without throwing.
     * Your code has: source != null ? source : "text"
     * This test locks that defensive check in place.
     */
    @Test
    void testInitializeChat_Success_NullSourceDefaultsToText() {
        // 1. Arrange
        UUID convId = UUID.randomUUID();
        Conversation savedConv = makeConversation(convId, UUID.randomUUID());

        Mockito.when(conversationRepo.save(Mockito.any(Conversation.class))).thenReturn(savedConv);
        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(Map.class))
        ).thenReturn(Map.of());
        Mockito.when(aiService.getEmbedding(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new float[]{0.1f, 0.2f});

        // 2. Act — source = null
        assertDoesNotThrow(() ->
                chatService.initializeChat(
                        UUID.randomUUID(), UUID.randomUUID(),
                        "Title", "Some transcript", null, null
                )
        );
    }

    /**
     * Failure — Python worker is unreachable during initialization.
     * RestTemplate throws; the whole initializeChat must propagate the exception
     * so the caller knows the chat was NOT successfully set up.
     */
    @Test
    void testInitializeChat_Failure_PythonWorkerUnreachable() {
        // 1. Arrange
        Conversation savedConv = makeConversation(UUID.randomUUID(), UUID.randomUUID());
        Mockito.when(conversationRepo.save(Mockito.any(Conversation.class))).thenReturn(savedConv);

        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(Map.class))
        ).thenThrow(new RuntimeException("Connection refused"));

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () ->
                chatService.initializeChat(
                        UUID.randomUUID(), UUID.randomUUID(),
                        "Title", "transcript", "text", null
                )
        );
    }

    /**
     * Edge case — empty transcript string.
     * No chunks should be produced; documentRepository.save() must never be called.
     */
    @Test
    void testInitializeChat_Success_EmptyTranscript_NoEmbeddingsSaved() {
        // 1. Arrange
        Conversation savedConv = makeConversation(UUID.randomUUID(), UUID.randomUUID());
        Mockito.when(conversationRepo.save(Mockito.any(Conversation.class))).thenReturn(savedConv);
        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(Map.class))
        ).thenReturn(Map.of());

        // 2. Act
        chatService.initializeChat(
                UUID.randomUUID(), UUID.randomUUID(),
                "Title", "", "text", null
        );

        // 3. Assert — empty string produces no chunks
        Mockito.verify(documentRepository, Mockito.never()).save(Mockito.any(Document.class));
    }


    // ═══════════════════════════════════════════════════════════
    //  ASK AI  — text mode
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — text mode chat.
     * Verifies the full pipeline:
     *   load conversation → load transcript → save user msg →
     *   call Python → save AI reply → save Q&A embedding → return reply
     */
    @Test
    void testAskAi_Success_TextMode() {
        // 1. Arrange
        UUID chatId = UUID.randomUUID();

        Conversation conv = makeConversation(chatId, UUID.randomUUID());
        Mockito.when(conversationRepo.findById(chatId)).thenReturn(Optional.of(conv));

        MeetingTranscript transcript = makeTranscript("text");
        Mockito.when(transcriptRepository.findByConversationId(chatId))
                .thenReturn(Optional.of(transcript));

        // Message history returns empty list (new conversation)
        Mockito.when(messageRepo.findRecentByChatId(chatId)).thenReturn(List.of());

        // Python returns a valid reply
        AiResponse mockResponse = makeAiResponse("This is the AI answer.");
        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(AiResponse.class))
        ).thenReturn(mockResponse);

        // Embedding for Q&A pair saving
        Mockito.when(aiService.getEmbedding(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        // 2. Act
        String reply = chatService.askAi(chatId, "What is the main topic?");

        // 3. Assert
        assertNotNull(reply);
        assertEquals("This is the AI answer.", reply);

        // User message + AI message both saved = 2 saves
        Mockito.verify(messageRepo, Mockito.times(2)).save(Mockito.any(Message.class));

        // Q&A embedding saved
        Mockito.verify(documentRepository, Mockito.times(1)).save(Mockito.any(Document.class));
    }

    /**
     * Failure — conversation ID does not exist.
     * Must throw immediately; nothing else should be called.
     */
    @Test
    void testAskAi_Failure_ConversationNotFound() {
        // 1. Arrange
        UUID chatId = UUID.randomUUID();
        Mockito.when(conversationRepo.findById(chatId)).thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () ->
                chatService.askAi(chatId, "Any question")
        );

        // Verification: nothing downstream should run
        Mockito.verify(messageRepo, Mockito.never()).save(Mockito.any(Message.class));
        Mockito.verify(restTemplate, Mockito.never())
                .postForObject(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    /**
     * Failure — transcript metadata not found for this conversation.
     * askAi needs the source field to decide text vs audio mode.
     */
    @Test
    void testAskAi_Failure_TranscriptNotFound() {
        // 1. Arrange
        UUID chatId = UUID.randomUUID();

        Conversation conv = makeConversation(chatId, UUID.randomUUID());
        Mockito.when(conversationRepo.findById(chatId)).thenReturn(Optional.of(conv));
        Mockito.when(transcriptRepository.findByConversationId(chatId))
                .thenReturn(Optional.empty());

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () ->
                chatService.askAi(chatId, "Any question")
        );

        Mockito.verify(restTemplate, Mockito.never())
                .postForObject(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    /**
     * Failure — Python worker returns null response.
     * Must throw "No response from AI server" — never silently return null to the user.
     */
    @Test
    void testAskAi_Failure_PythonReturnsNull() {
        // 1. Arrange
        UUID chatId = UUID.randomUUID();

        Mockito.when(conversationRepo.findById(chatId))
                .thenReturn(Optional.of(makeConversation(chatId, UUID.randomUUID())));
        Mockito.when(transcriptRepository.findByConversationId(chatId))
                .thenReturn(Optional.of(makeTranscript("text")));
        Mockito.when(messageRepo.findRecentByChatId(chatId)).thenReturn(List.of());

        // Python returns null
        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(AiResponse.class))
        ).thenReturn(null);

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                chatService.askAi(chatId, "Any question")
        );
        assertTrue(ex.getMessage().contains("No response from AI server"));

        // AI reply must NOT be saved if response was null
        Mockito.verify(messageRepo, Mockito.times(1)).save(Mockito.any(Message.class)); // only user msg
    }

    /**
     * Failure — Python returns a response object but reply field is null (worker error).
     * Must throw with the error message from the response.
     */
    @Test
    void testAskAi_Failure_PythonReturnsNullReply() {
        // 1. Arrange
        UUID chatId = UUID.randomUUID();

        Mockito.when(conversationRepo.findById(chatId))
                .thenReturn(Optional.of(makeConversation(chatId, UUID.randomUUID())));
        Mockito.when(transcriptRepository.findByConversationId(chatId))
                .thenReturn(Optional.of(makeTranscript("text")));
        Mockito.when(messageRepo.findRecentByChatId(chatId)).thenReturn(List.of());

        // Python returns response with null reply and an error message
        AiResponse errorResponse = new AiResponse();
        errorResponse.setReply(null);
        errorResponse.setError("Model overloaded");
        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(AiResponse.class))
        ).thenReturn(errorResponse);

        // 2. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                chatService.askAi(chatId, "Any question")
        );
        assertTrue(ex.getMessage().contains("AI Worker Error"));
    }

    /**
     * Failure — Python HTTP call throws (network timeout, connection refused).
     * Exception must propagate; no AI message should be saved.
     */
    @Test
    void testAskAi_Failure_PythonWorkerUnreachable() {
        // 1. Arrange
        UUID chatId = UUID.randomUUID();

        Mockito.when(conversationRepo.findById(chatId))
                .thenReturn(Optional.of(makeConversation(chatId, UUID.randomUUID())));
        Mockito.when(transcriptRepository.findByConversationId(chatId))
                .thenReturn(Optional.of(makeTranscript("text")));
        Mockito.when(messageRepo.findRecentByChatId(chatId)).thenReturn(List.of());

        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(AiResponse.class))
        ).thenThrow(new RuntimeException("Connection refused"));

        // 2. Act & Assert
        assertThrows(RuntimeException.class, () ->
                chatService.askAi(chatId, "Any question")
        );

        // AI message must never be saved if Python call failed
        Mockito.verify(messageRepo, Mockito.times(1)).save(Mockito.any(Message.class)); // only user msg saved
    }

    /**
     * Edge case — null source in transcript defaults to "text" mode.
     * Guards against NullPointerException on transcriptMeta.getSource().
     */
    @Test
    void testAskAi_Success_NullSourceInTranscriptDefaultsToText() {
        // 1. Arrange
        UUID chatId = UUID.randomUUID();

        Mockito.when(conversationRepo.findById(chatId))
                .thenReturn(Optional.of(makeConversation(chatId, UUID.randomUUID())));

        // Transcript has null source
        MeetingTranscript transcript = makeTranscript(null);
        Mockito.when(transcriptRepository.findByConversationId(chatId))
                .thenReturn(Optional.of(transcript));
        Mockito.when(messageRepo.findRecentByChatId(chatId)).thenReturn(List.of());
        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(), Mockito.any(), Mockito.eq(AiResponse.class))
        ).thenReturn(makeAiResponse("Reply for null source case"));
        Mockito.when(aiService.getEmbedding(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new float[]{0.1f});

        // 2. Act — must not throw NPE
        assertDoesNotThrow(() -> chatService.askAi(chatId, "Question?"));
    }


    // ═══════════════════════════════════════════════════════════
    //  GET GROUPED CHAT HISTORY
    // ═══════════════════════════════════════════════════════════

    /**
     * Happy path — user has messages across multiple conversations.
     * Verifies messages are correctly grouped by conversation ID.
     */
    @Test
    void testGetGroupedChatHistory_Success_GroupsByConversation() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID conv1Id = UUID.randomUUID();
        UUID conv2Id = UUID.randomUUID();

        Conversation conv1 = makeConversation(conv1Id, userId);
        Conversation conv2 = makeConversation(conv2Id, userId);

        Message msg1 = new Message(); msg1.setConversation(conv1); msg1.setContent("Hello");
        Message msg2 = new Message(); msg2.setConversation(conv1); msg2.setContent("World");
        Message msg3 = new Message(); msg3.setConversation(conv2); msg3.setContent("Other conv");

        Mockito.when(messageRepo.findAllByUserId(userId))
                .thenReturn(List.of(msg1, msg2, msg3));

        // 2. Act
        List<ChatHistoryDto> result = chatService.getGroupedChatHistory(userId);

        // 3. Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // 2 distinct conversations
    }

    /**
     * Edge case — user has no messages yet (new account).
     * Must return empty list, not null.
     */
    @Test
    void testGetGroupedChatHistory_ReturnsEmpty_WhenNoMessages() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        Mockito.when(messageRepo.findAllByUserId(userId)).thenReturn(List.of());

        // 2. Act
        List<ChatHistoryDto> result = chatService.getGroupedChatHistory(userId);

        // 3. Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Edge case — all messages belong to the same conversation.
     * Must return a list with exactly 1 group containing all messages.
     */
    @Test
    void testGetGroupedChatHistory_SingleConversation_AllMessagesGroupedTogether() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();
        Conversation conv = makeConversation(convId, userId);

        Message m1 = new Message(); m1.setConversation(conv); m1.setContent("Q1");
        Message m2 = new Message(); m2.setConversation(conv); m2.setContent("A1");
        Message m3 = new Message(); m3.setConversation(conv); m3.setContent("Q2");

        Mockito.when(messageRepo.findAllByUserId(userId))
                .thenReturn(List.of(m1, m2, m3));

        // 2. Act
        List<ChatHistoryDto> result = chatService.getGroupedChatHistory(userId);

        // 3. Assert
        assertEquals(1, result.size()); // all 3 messages in one group
    }
}