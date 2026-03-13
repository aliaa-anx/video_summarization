package com.backend_microservices.ai_service.service;
import com.backend_microservices.ai_service.dto.AiResponse;
import com.backend_microservices.ai_service.dto.ChatRequest;
import com.backend_microservices.ai_service.dto.MessageDTO;
import com.backend_microservices.ai_service.entity.Message;
import com.backend_microservices.ai_service.entity.Conversation;
import com.backend_microservices.ai_service.repository.ConversationRepository;
import com.backend_microservices.ai_service.repository.DocumentRepository;
import com.backend_microservices.ai_service.repository.MessageRepository;
import lombok.AllArgsConstructor;
import com.backend_microservices.ai_service.entity.Document;
import com.backend_microservices.ai_service.entity.Conversation;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class ChatService {
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final AiService aiService;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;
    //private final AiResponse response;
    @Value("${python.ai.url}")
    private String pythonNgrokUrl;
    /**
     * Step 1: Initialize a new video chat
     */
//    @Transactional
//    public void initializeChat(UUID userId, String videoTitle, String transcript) {
//        // Create the conversation record
//        Conversation conversation = new Conversation();
//        conversation.setUserId(userId);
//        conversation.setTitle(videoTitle);
//        Conversation savedConv = conversationRepo.save(conversation);
//        //AiResponse response = aiService.initializeVideoContent(savedConv.getId(), transcript);
//        // Tell Python to index this transcript
//        aiService.initializeVideoContent(savedConv.getId(), transcript);
//        saveEmbeddings(savedConv.getId(), response.getPayload());
//    }



    @Transactional
    public void initializeChat(UUID userId, String videoTitle, String transcript) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(videoTitle);
        Conversation savedConv = conversationRepo.save(conversation);

        AiResponse response = aiService.initializeVideoContent(savedConv.getId(), transcript);
        // ADD THESE LOGS
        System.out.println("AI Response: " + response);
        System.out.println("Payload: " + response.getPayload());
        System.out.println("Payload size: " + (response.getPayload() != null ? response.getPayload().size() : "NULL"));

        saveEmbeddings(savedConv.getId(), response.getPayload());
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
        List<String> context = documentRepository.findRelevantContext(chatId, queryVector);

        // 5. Fetch history (now includes the message we just saved)
        List<MessageDTO> history = messageRepo.findRecentByChatId(chatId);

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
        saveEmbeddings(chatId, response.getPayload());

        return response.getReply();
    }


    /**
     * Step 2: Process a message (Save -> Call AI -> Save -> Return)
     */
//    @Transactional
//    public String processUserMessage(UUID chatId, String userContent) {
//        Conversation conv = conversationRepo.findById(chatId)
//                .orElseThrow(() -> new RuntimeException("Conversation not found"));
//
//        // 1. Save User Message
//        Message userMsg = new Message();
//        userMsg.setConversation(conv);
//        userMsg.setRole("user");
//        userMsg.setContent(userContent);
//        messageRepo.save(userMsg);
//
//        // 2. Get AI Response from AiService (which uses RestTemplate)
//        String aiReply = aiService.getReplyFromPython(chatId, userContent);
//        // Add this temporarily to see what's coming back
//        System.out.println("AI REPLY: " + aiReply);
//
//        if (aiReply == null || aiReply.isBlank()) {
//            throw new RuntimeException("AI service returned empty response");
//        }
//        // 3. Save AI Message
//        Message aiMsg = new Message();
//        aiMsg.setConversation(conv);
//        aiMsg.setRole("assistant");
//        aiMsg.setContent(aiReply);
//        messageRepo.save(aiMsg);
//
//        return aiReply;
//    }
    /**
     * Step 3: Get history for the UI
     */
    public List<Message> getChatHistory(UUID chatId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(chatId);
    }
}
