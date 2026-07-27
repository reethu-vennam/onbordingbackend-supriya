package com.sabbpe.repository;

import com.sabbpe.model.ChatAudioLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatAudioLogRepository extends JpaRepository<ChatAudioLogEntity, String> {

    List<ChatAudioLogEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ChatAudioLogEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    Optional<ChatAudioLogEntity> findById(String id);
}
