package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.entity.MeetingTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;
public interface MeetingTranscriptRepository
        extends JpaRepository<MeetingTranscript, UUID> {

    @Query("SELECT m.userId, COUNT(m) FROM MeetingTranscript m GROUP BY m.userId ORDER BY COUNT(m) DESC")
    List<Object[]> getTopUsersByMeetings(Pageable pageable);


    // Native query to handle cross-schema JOIN between 'public' and 'chat'
    @Query(value = "SELECT mt.* FROM public.meeting_transcripts mt " +
            "JOIN chat.conversations c ON mt.id = c.meeting_id " +
            "WHERE c.id = :conversationId", nativeQuery = true)
    Optional<MeetingTranscript> findByConversationId(@Param("conversationId") UUID conversationId);

}
