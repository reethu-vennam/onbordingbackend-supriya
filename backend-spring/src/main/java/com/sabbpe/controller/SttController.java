package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.model.ChatAudioLogEntity;
import com.sabbpe.repository.ChatAudioLogRepository;
import com.sabbpe.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
public class SttController {

    private final ChatAudioLogRepository chatAudioLogRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> transcribe(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> request) {

        String audioPath = request.get("audioPath") != null ? request.get("audioPath").toString() : null;
        String language = request.get("language") != null ? request.get("language").toString() : "en";

        ChatAudioLogEntity logEntry = new ChatAudioLogEntity();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setUserId(user != null ? user.getId() : null);
        logEntry.setSessionId(UUID.randomUUID().toString());
        logEntry.setAudioStoragePath(audioPath);
        logEntry.setTranscript("[transcription pending]");
        logEntry.setLanguage(language);
        chatAudioLogRepository.save(logEntry);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "logId", logEntry.getId(),
                "transcript", "[transcription pending]",
                "language", language
        )));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLogs(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<ChatAudioLogEntity> logs = chatAudioLogRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> result = logs.stream()
                .map(l -> Map.<String, Object>of(
                        "id", l.getId(),
                        "sessionId", l.getSessionId(),
                        "transcript", l.getTranscript(),
                        "language", l.getLanguage(),
                        "createdAt", l.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/audio/{logId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAudio(@PathVariable String logId) {
        ChatAudioLogEntity logEntry = chatAudioLogRepository.findById(logId).orElse(null);
        if (logEntry == null) {
            return ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "Audio log not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", logEntry.getId(),
                "audioPath", logEntry.getAudioStoragePath(),
                "transcript", logEntry.getTranscript(),
                "language", logEntry.getLanguage()
        )));
    }

    @GetMapping("/audio/play/{logId}")
    public ResponseEntity<Resource> playAudio(@PathVariable String logId) {
        ChatAudioLogEntity logEntry = chatAudioLogRepository.findById(logId).orElse(null);
        if (logEntry == null || logEntry.getAudioStoragePath() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path audioPath = Paths.get(logEntry.getAudioStoragePath());
            Resource resource = new UrlResource(audioPath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/wav"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            }
        } catch (MalformedURLException e) {
            log.error("Error reading audio file", e);
        }
        return ResponseEntity.notFound().build();
    }
}
