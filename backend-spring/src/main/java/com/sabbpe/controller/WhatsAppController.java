package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.SendResult;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.WhatsAppService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final Environment environment;

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_TTL_MS = 5 * 60 * 1000;

    @Value("${app.msg91.template-name}")
    private String defaultTemplateName;

    @Value("${app.msg91.namespace}")
    private String defaultNamespace;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> body) {
        String to = body.get("to");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("MISSING_TO", "Missing \"to\" phone number"));
        }

        String templateName = body.getOrDefault("templateName", defaultTemplateName);
        String namespace = body.getOrDefault("namespace", defaultNamespace);

        log.info("OTP request for user: {}, to: {}", user.getId(), to);

        String otp = generateOtp(4);
        otpStore.put(to, new OtpEntry(otp, System.currentTimeMillis() + OTP_TTL_MS));

        SendResult result = whatsAppService.sendOtp(to, templateName, namespace, otp);

        if (!result.isSuccess()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("error", result.getError());
            if (result.getStatus() != null) {
                data.put("providerResponse", result.getStatus());
            }
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("SEND_FAILED", result.getError(), data));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("otpSent", true);
        if (result.getStatus() != null) {
            data.put("providerResponse", result.getStatus());
        }

        if (isDevMode()) {
            data.put("debugOtp", otp);
        }

        return ResponseEntity.ok(ApiResponse.success("OTP sent", data));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> body) {
        String to = body.get("to");
        String enteredOtp = body.get("otp");

        if (to == null || enteredOtp == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("MISSING_PARAMS", "Missing required parameters"));
        }

        OtpEntry entry = otpStore.get(to);
        if (entry == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("NO_OTP", "No OTP requested for this number"));
        }

        if (System.currentTimeMillis() > entry.getExpiresAt()) {
            otpStore.remove(to);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("OTP_EXPIRED", "OTP expired"));
        }

        if (entry.getOtp().equals(enteredOtp.trim())) {
            otpStore.remove(to);
            return ResponseEntity.ok(ApiResponse.success("OTP verified",
                    Map.of("verified", true)));
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("OTP_MISMATCH", "OTP mismatch"));
    }

    private String generateOtp(int digits) {
        int min = (int) Math.pow(10, digits - 1);
        int max = (int) Math.pow(10, digits) - 1;
        return String.valueOf(min + (int) (Math.random() * (max - min + 1)));
    }

    private boolean isDevMode() {
        String debugOtp = environment.getProperty("DEBUG_INCLUDE_OTP");
        if ("true".equalsIgnoreCase(debugOtp)) {
            return true;
        }
        String nodeEnv = environment.getProperty("NODE_ENV");
        if ("development".equalsIgnoreCase(nodeEnv)) {
            return true;
        }
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @Data
    @AllArgsConstructor
    private static class OtpEntry {
        private String otp;
        private long expiresAt;
    }
}
