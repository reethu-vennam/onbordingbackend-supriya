package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.ChatRequest;
import com.sabbpe.dto.ChatResponse;
import com.sabbpe.model.ChatAudioLogEntity;
import com.sabbpe.repository.ChatAudioLogRepository;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.ChatbotService;
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

    private final ChatbotService chatbotService;
    private final ChatAudioLogRepository chatAudioLogRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ChatRequest request) {

        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        String message = request.getMessage() != null ? request.getMessage() : "";
        String currentStep = request.getCurrentStep() != null ? request.getCurrentStep() : "welcome";
        String language = request.getLanguage() != null ? request.getLanguage() : "en";

        ChatAudioLogEntity logEntry = new ChatAudioLogEntity();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setUserId(user != null ? user.getId() : null);
        logEntry.setSessionId(sessionId);
        logEntry.setTranscript(message);
        logEntry.setLanguage(language);
        logEntry.setOnboardingStep(currentStep);
        chatAudioLogRepository.save(logEntry);

        ChatResponse response = chatbotService.processMessage(sessionId, message, currentStep, language);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reset(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> request) {

        String sessionId = request.get("sessionId") != null ? request.get("sessionId").toString() : null;
        if (sessionId != null) {
            chatbotService.resetSession(sessionId);
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "sessionId", sessionId,
                "reset", true,
                "message", "Session reset successfully"
        )));
    }
}
