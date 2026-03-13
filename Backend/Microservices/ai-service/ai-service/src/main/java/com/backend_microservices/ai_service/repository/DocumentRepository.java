package com.backend_microservices.ai_service.repository;
import com.backend_microservices.ai_service.entity.Document;
import com.backend_microservices.ai_service.entity.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document,Long> {
    // Check if we already have chunks for a specific video
    //List<Document> findBySourceName(String sourceName);

    // Delete chunks if a video is removed or re-uploaded
    //void deleteBySourceName(String sourceName);

    @Query(value = "SELECT content FROM rag.documents " +
            "WHERE conversation_id = :chatId " +
            "ORDER BY embedding <=> cast(:queryVector as vector) " +
            "LIMIT 5", nativeQuery = true)
    List<String> findRelevantContext(@Param("chatId") UUID chatId, @Param("queryVector") float[] queryVector);
}
