package com.sabbpe.controller;

import com.sabbpe.dto.DemoQuotaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/quota/increment")
    public ResponseEntity<Map<String, Object>> incrementQuota(
            @RequestBody DemoQuotaRequest request,
            @RequestHeader(value = "x-demo-key", required = false) String demoKey) {
        log.info("Demo quota increment: type={}, status={}", request.getCheckType(), request.getStatus());

        String key = request.getCheckType() != null ? request.getCheckType() : "default";
        AtomicInteger counter = quotaStore.computeIfAbsent(key, k -> new AtomicInteger(0));
        int used = counter.incrementAndGet();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "used", used,
                "max", DEFAULT_MAX_QUOTA
        ));
    }
}
