package com.sabbpe.controller;

import com.sabbpe.dto.DemoQuotaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final Map<String, AtomicInteger> quotaStore = new ConcurrentHashMap<>();
    private static final int DEFAULT_MAX_QUOTA = 50;
    private static final String DEFAULT_KEY = "default";

    @Value("${app.demo.access-key:}")
    private String demoAccessKey;

    @GetMapping("/quota")
    public ResponseEntity<Map<String, Object>> getQuota(
            @RequestHeader(value = "x-demo-key", required = false) String demoKey) {
        ResponseEntity<Map<String, Object>> denied = checkDemoKey(demoKey);
        if (denied != null) return denied;

        int used = quotaStore.computeIfAbsent(DEFAULT_KEY, k -> new AtomicInteger(0)).get();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "used", used,
                "max", DEFAULT_MAX_QUOTA
        ));
    }

    @PostMapping("/quota/increment")
    public ResponseEntity<Map<String, Object>> incrementQuota(
            @RequestBody DemoQuotaRequest request,
            @RequestHeader(value = "x-demo-key", required = false) String demoKey) {
        ResponseEntity<Map<String, Object>> denied = checkDemoKey(demoKey);
        if (denied != null) return denied;

        log.info("Demo quota increment: type={}, status={}", request.getCheckType(), request.getStatus());

        String key = request.getCheckType() != null ? request.getCheckType() : DEFAULT_KEY;
        AtomicInteger counter = quotaStore.computeIfAbsent(key, k -> new AtomicInteger(0));
        int used = counter.incrementAndGet();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "used", used,
                "max", DEFAULT_MAX_QUOTA
        ));
    }

    // Mirrors Node's requireDemoKey middleware (backend/src/routes/demo.ts): reject if the
    // server has no configured key, and reject if the caller's key doesn't match.
    private ResponseEntity<Map<String, Object>> checkDemoKey(String provided) {
        if (demoAccessKey == null || demoAccessKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "DEMO_ACCESS_KEY not configured on server"));
        }
        if (provided == null || !provided.equals(demoAccessKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid or missing demo access key"));
        }
        return null;
    }
}
