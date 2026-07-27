package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "uptime", System.currentTimeMillis()
        )));
    }
}
