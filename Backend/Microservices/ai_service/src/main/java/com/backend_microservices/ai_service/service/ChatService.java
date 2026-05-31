package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.entity.*;
import com.backend_microservices.ai_service.repository.ConversationRepository;
import com.backend_microservices.ai_service.repository.DocumentRepository;
import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RequiredArgsConstructor
@Service
public class ChatService {
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final AiService aiService;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;
    private final MessageRepository messageRepository;
    private final MeetingTranscriptRepository transcriptRepository;
    //private final AiResponse response;
    @Value("${python.ai.url}")
    private String pythonNgrokUrl;
    /**
     * Step 1: Initialize a new video chat
     */


    @Transactional
    public String initializeChat(UUID userId, UUID meetingId, String videoTitle, String correctedTranscript, String source, String segmentsJson) {
        // 1. Create new conversation and SET THE MEETING ID
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setMeetingId(meetingId); // <--- CRITICAL FIX: Link to public.meeting_transcripts
        conversation.setTitle(videoTitle);

        Conversation savedConv = conversationRepo.save(conversation);

        // 2. Notify Python worker
        Map<String, Object> pythonPayload = new HashMap<>();
        //pythonPayload.put("source", source);
        pythonPayload.put("source", source != null ? source : "text");
        pythonPayload.put("segments", List.of());
        pythonPayload.put("corrected_text", correctedTranscript);

        String initUrl = pythonNgrokUrl + "/chat/" + savedConv.getId() + "/init";
        restTemplate.postForObject(initUrl, pythonPayload, Map.class);

        // 3. Perform Embeddings & Storage
        if ("audio".equals(source)) {
            saveAudioSegments(savedConv, segmentsJson);
        } else {
            saveTranscriptEmbeddings(savedConv, correctedTranscript);
        }

        return savedConv.getId().toString();
    }

    private void saveAudioSegments(Conversation conv, String segmentsJson) {
        try {
            List<Map<String, Object>> segments = new ObjectMapper()
                    .readValue(segmentsJson, List.class);

            int chunkIndex = 0;
            for (Map<String, Object> seg : segments) {
                String text = (String) seg.get("text");
                Double start = ((Number) seg.get("start")).doubleValue();
                Double end = ((Number) seg.get("end")).doubleValue();

                float[] embedding = aiService.getEmbedding(text, "passage");

                Document doc = new Document();
                doc.setConversation(conv);
                doc.setContent(text);
                doc.setEmbedding(embedding);
                doc.setStartTime(start);
                doc.setEndTime(end);
                doc.setChunkIndex(chunkIndex++);
                documentRepository.save(doc);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save audio segments: " + e.getMessage());
        }
    }
    private void saveEmbeddings(UUID chatId, List<AiResponse.EmbeddingPayload> payload) {
        if (payload == null) return;

        // Get the conversation reference
        Conversation conversation = conversationRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        List<Document> documents = payload.stream().map(p -> {
            Document doc = new Document();
            doc.setConversation(conversation);
            doc.setContent(p.getContent());
            doc.setEmbedding(p.getEmbedding());
            return doc;
        }).toList();

        documentRepository.saveAll(documents);
    }

    // Saves transcript chunks as embeddings for text mode
    private void saveTranscriptEmbeddings(Conversation conv, String transcript) {
        // Split into chunks of ~500 chars with overlap
        int chunkSize = 500;
        int overlap = 100;
        int i = 0;
        int chunkIndex = 0;

        while (i < transcript.length()) {
            int end = Math.min(i + chunkSize, transcript.length());
            String chunk = transcript.substring(i, end);

            float[] embedding = aiService.getEmbedding(chunk, "passage");

            Document doc = new Document();
            doc.setConversation(conv);
            doc.setContent(chunk);
            doc.setEmbedding(embedding);
            doc.setChunkIndex(chunkIndex);
            doc.setStartTime(null);
            doc.setEndTime(null);
            documentRepository.save(doc);

            i += (chunkSize - overlap);
            chunkIndex++;
        }
    }



//    @Transactional
//    public String askAi(UUID chatId, String userMessage) {
//        // 1. Get Conversation object (needed for saving messages later)
//        Conversation conv = conversationRepo.findById(chatId)
//                .orElseThrow(() -> new RuntimeException("Conversation not found"));
//
//        // 2. SAVE the User's text message to DB history
//        Message userMsg = new Message();
//        userMsg.setConversation(conv);
//        userMsg.setRole("user");
//        userMsg.setContent(userMessage);
//        messageRepo.save(userMsg);
//
//        // 3. Get the embedding for the search
//        float[] queryVector = aiService.getEmbedding(userMessage, "query");
//
//        // 4. Fetch private context from PGVector
//        //List<String> context = documentRepository.findRelevantContextWithTimeStamps(chatId, queryVector);
//
//        // 5. Fetch history
//        List<MessageDTO> history = messageRepo.findRecentByChatId(chatId);
//
//        List<Object[]> rawContext = documentRepository
//                .findRelevantContextWithTimeStamps(chatId, queryVector);
//
//        // Map to ContextChunk DTOs
//        List<ContextChunk> context = rawContext.stream()
//                .map(row -> new ContextChunk(
//                        (String) row[0], // content
//                        row[1] != null ? ((Number) row[1]).doubleValue() : null, // startTime
//                        row[2] != null ? ((Number) row[2]).doubleValue() : null  // endTime
//                ))
//                .toList();
//
//        // 6. Call Python via ngrok
//        ChatRequest request = new ChatRequest(userMessage, history, context);
//        String fullUrl = pythonNgrokUrl + "/chat/" + chatId + "/ask";
//        AiResponse response = restTemplate.postForObject(fullUrl, request, AiResponse.class);
//
//        if (response == null) throw new RuntimeException("AI Response was null");
//
//        // 7. SAVE the AI's reply to DB history
//        Message aiMsg = new Message();
//        aiMsg.setConversation(conv);
//        aiMsg.setRole("assistant");
//        aiMsg.setContent(response.getReply());
//        messageRepo.save(aiMsg);
//
//        // 8. SAVE the new embeddings for future RAG
//        //saveEmbeddings(chatId, response.getPayload());
//        saveQAPairEmbedding(conv, userMessage, response.getReply());
//
//        return response.getReply();
//    }
@Transactional
public String askAi(UUID chatId, String userMessage) {
    // 1. Load conversation and meeting metadata
    Conversation conv = conversationRepo.findById(chatId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

    MeetingTranscript transcriptMeta = transcriptRepository.findByConversationId(chatId)
            .orElseThrow(() -> new RuntimeException("Meeting Transcript not found"));

    // 2. Save user message
    Message userMsg = new Message();
    userMsg.setConversation(conv);
    userMsg.setRole("user");
    userMsg.setContent(userMessage);
    messageRepo.save(userMsg);

    // 3. Build request for Python
    ChatRequest request = new ChatRequest();
    request.setMessage(userMessage);
    String safeSource = transcriptMeta.getSource() != null ? transcriptMeta.getSource() : "text";
    request.setSource(safeSource);
    //request.setSource(transcriptMeta.getSource());
    request.setContext_segments(new ArrayList<>());
    // Only include messages with non-null content in history
    List<MessageDTO> cleanHistory = messageRepo.findRecentByChatId(chatId)
            .stream()
            .map(row -> new MessageDTO((String) row[0], (String) row[1]))
            .collect(Collectors.toList());

// Reverse so oldest is first (correct order for LLM)
    Collections.reverse(cleanHistory);
    request.setMessage_history(cleanHistory);
    if ("audio".equals(transcriptMeta.getSource())) {
        request.setContext_segments(performDualVectorSearch(chatId, userMessage));
    } else {
        request.setFull_text(transcriptMeta.getCorrectedTranscript());
    }

    // 4. Call Python
    String askUrl = pythonNgrokUrl + "/chat/ask";
    AiResponse response = restTemplate.postForObject(askUrl, request, AiResponse.class);

    // 5. Check response FIRST before doing anything
    if (response == null) {
        throw new RuntimeException("No response from AI server");
    }

    if (response.getReply() == null) {
        throw new RuntimeException("AI Worker Error: " + response.getError());
    }


    // 6. Only save if reply is valid
    Message aiMsg = new Message();
    aiMsg.setConversation(conv);
    aiMsg.setRole("assistant");
    aiMsg.setContent(response.getReply());
    messageRepo.save(aiMsg);

    // 7. Save Q&A embedding for future retrieval
    saveQAPairEmbedding(conv, userMessage, response.getReply());

    return response.getReply();
}

    /**
     * Replicates the Dual-Search & Merge logic from your notebook
     */
    private List<ContextChunk> performDualVectorSearch(UUID chatId, String userMessage) {
        // Search 1: Full Question
        float[] queryVector = aiService.getEmbedding(userMessage, "query");
        List<ScoredChunkDto> search1 = mapToScoredDto(documentRepository.findRelevantContextWithTimeStamps(chatId, queryVector));

        // Search 2: Keywords
        String keywords = userMessage
                .replaceAll("(?i)ايه|إيه|ما هو|ما هي|إزاي|كيف|what is|how to|what are|explain|الفرق بين|difference between", "")
                .trim();
        float[] keywordsVector = aiService.getEmbedding(keywords, "query");
        List<ScoredChunkDto> search2 = mapToScoredDto(documentRepository.findRelevantContextWithTimeStamps(chatId, keywordsVector));

        // Merge & Deduplicate by content key
        Map<String, ScoredChunkDto> deduplicated = new HashMap<>();
        Stream.concat(search1.stream(), search2.stream()).forEach(chunk ->
                deduplicated.merge(chunk.getContent(), chunk, (existing, incoming) ->
                        incoming.getScore() < existing.getScore() ? incoming : existing)
        );

        // Return top 8 sorted by score/distance
        return deduplicated.values().stream()
                .sorted(Comparator.comparingDouble(ScoredChunkDto::getScore))
                .limit(8)
                .map(s -> new ContextChunk(s.getContent(), s.getStartTime(), s.getEndTime()))
                .toList();
    }

    private List<ScoredChunkDto> mapToScoredDto(List<Object[]> rows) {
        return rows.stream().map(row -> new ScoredChunkDto(
                (String) row[0],
                row[1] != null ? ((Number) row[1]).doubleValue() : null,
                row[2] != null ? ((Number) row[2]).doubleValue() : null,
                ((Number) row[3]).doubleValue() // distance
        )).toList();
    }


    private void saveQAPairEmbedding(Conversation conv, String question, String answer) {
        float[] embedding = aiService.getEmbedding(question + " " + answer, "passage");
        Document doc = new Document();
        doc.setConversation(conv);
        doc.setContent("Q: " + question + "\nA: " + answer);
        doc.setEmbedding(embedding);
        // No timestamps for Q&A pairs
        doc.setStartTime(null);
        doc.setEndTime(null);
        documentRepository.save(doc);}

    /**
     * Step 3: Get history for the UI
     */
//    public List<Message> getChatHistory(UUID chatId) {
//        return messageRepo.findByConversationIdOrderByCreatedAtAsc(chatId);
//    }

    public List<ChatHistoryDto> getGroupedChatHistory(UUID userId) {
        // 1. Fetch all messages belonging to this user's conversations
        List<Message> allMessages = messageRepository.findAllByUserId(userId);

        // 2. Group them by conversation ID to create the separate array entries
        return allMessages.stream()
                .collect(Collectors.groupingBy(m -> m.getConversation().getId()))
                .entrySet().stream()
                .map(entry -> new ChatHistoryDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }


}
