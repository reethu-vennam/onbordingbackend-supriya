package com.sabbpe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_audio_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatAudioLogEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "audio_storage_path", columnDefinition = "TEXT")
    private String audioStoragePath;

    @Column(name = "transcript", nullable = false, columnDefinition = "TEXT")
    private String transcript = "";

    @Column(name = "language", nullable = false, length = 10)
    private String language = "en";

    @Column(name = "onboarding_step", length = 100)
    private String onboardingStep;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (sessionId == null) sessionId = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }
}
