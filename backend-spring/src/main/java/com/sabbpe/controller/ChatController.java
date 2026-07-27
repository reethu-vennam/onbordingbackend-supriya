package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.model.ChatAudioLogEntity;
import com.sabbpe.repository.ChatAudioLogRepository;
import com.sabbpe.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatAudioLogRepository chatAudioLogRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> request) {

        String message = request.get("message") != null ? request.get("message").toString() : "";
        String language = request.get("language") != null ? request.get("language").toString() : "en";
        String sessionId = request.get("sessionId") != null ? request.get("sessionId").toString() : UUID.randomUUID().toString();

        ChatAudioLogEntity logEntry = new ChatAudioLogEntity();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setUserId(user != null ? user.getId() : null);
        logEntry.setSessionId(sessionId);
        logEntry.setTranscript(message);
        logEntry.setLanguage(language);
        chatAudioLogRepository.save(logEntry);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "sessionId", sessionId,
                "reply", "Message received: " + message,
                "language", language
        )));
    }
}
