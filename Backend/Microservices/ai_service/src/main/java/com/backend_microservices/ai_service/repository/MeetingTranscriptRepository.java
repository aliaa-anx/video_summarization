package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.entity.MeetingTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface MeetingTranscriptRepository
        extends JpaRepository<MeetingTranscript, UUID> {

    @Query("SELECT m.userId, COUNT(m) FROM MeetingTranscript m GROUP BY m.userId ORDER BY COUNT(m) DESC")
    List<Object[]> getTopUsersByMeetings(Pageable pageable);
}
