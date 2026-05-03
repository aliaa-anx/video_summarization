package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.dto.*;
import com.backend_microservices.ai_service.entity.ChatMessage;
import com.backend_microservices.ai_service.entity.Conversation;
import com.backend_microservices.ai_service.entity.Document;
import com.backend_microservices.ai_service.entity.Message;
import com.backend_microservices.ai_service.repository.ConversationRepository;
import com.backend_microservices.ai_service.repository.DocumentRepository;
import com.backend_microservices.ai_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class ChatService {
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final AiService aiService;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;
    private final MessageRepository messageRepository;
    //private final AiResponse response;
    @Value("${python.ai.url}")
    private String pythonNgrokUrl;
    /**
     * Step 1: Initialize a new video chat
     */


    @Transactional
    public String initializeChat(UUID userId, String videoTitle, String correctedTranscript,String source, String segmentsJson) {
        //create new conv
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(videoTitle);
        Conversation savedConv = conversationRepo.save(conversation);


        Map<String, Object> pythonPayload = new HashMap<>();
        pythonPayload.put("source", source);
        String initUrl = pythonNgrokUrl + "/chat/" + savedConv.getId() + "/init";
        restTemplate.postForObject(initUrl, pythonPayload, Map.class);

        if ("audio".equals(source)) {
            // save to PGVector
            saveAudioSegments(savedConv, segmentsJson);
        } else {
            pythonPayload.put("segments", List.of());
            pythonPayload.put("corrected_text", correctedTranscript);

            // For text mode: Java builds and saves embeddings itself
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

                float[] embedding = aiService.getEmbedding(text);

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

            float[] embedding = aiService.getEmbedding(chunk);

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



    @Transactional
    public String askAi(UUID chatId, String userMessage) {
        // 1. Get Conversation object (needed for saving messages later)
        Conversation conv = conversationRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // 2. SAVE the User's text message to DB history
        Message userMsg = new Message();
        userMsg.setConversation(conv);
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        messageRepo.save(userMsg);

        // 3. Get the embedding for the search
        float[] queryVector = aiService.getEmbedding(userMessage);

        // 4. Fetch private context from PGVector
        //List<String> context = documentRepository.findRelevantContextWithTimeStamps(chatId, queryVector);

        // 5. Fetch history
        List<MessageDTO> history = messageRepo.findRecentByChatId(chatId);

        List<Object[]> rawContext = documentRepository
                .findRelevantContextWithTimeStamps(chatId, queryVector);

        // Map to ContextChunk DTOs
        List<ContextChunk> context = rawContext.stream()
                .map(row -> new ContextChunk(
                        (String) row[0], // content
                        row[1] != null ? ((Number) row[1]).doubleValue() : null, // startTime
                        row[2] != null ? ((Number) row[2]).doubleValue() : null  // endTime
                ))
                .toList();

        // 6. Call Python via ngrok
        ChatRequest request = new ChatRequest(userMessage, history, context);
        String fullUrl = pythonNgrokUrl + "/chat/" + chatId + "/ask";
        AiResponse response = restTemplate.postForObject(fullUrl, request, AiResponse.class);

        if (response == null) throw new RuntimeException("AI Response was null");

        // 7. SAVE the AI's reply to DB history
        Message aiMsg = new Message();
        aiMsg.setConversation(conv);
        aiMsg.setRole("assistant");
        aiMsg.setContent(response.getReply());
        messageRepo.save(aiMsg);

        // 8. SAVE the new embeddings for future RAG
        //saveEmbeddings(chatId, response.getPayload());
        saveQAPairEmbedding(conv, userMessage, response.getReply());

        return response.getReply();
    }


    private void saveQAPairEmbedding(Conversation conv, String question, String answer) {
        float[] embedding = aiService.getEmbedding(question + " " + answer);
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
