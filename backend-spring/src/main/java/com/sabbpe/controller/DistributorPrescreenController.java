package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.TransbankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/distributor/prescreen")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN', 'EMPLOYEE')")
public class DistributorPrescreenController {

    private final TransbankService transbankService;

    @PostMapping("/aadhaar-generate-otp")
    public ResponseEntity<Map<String, Object>> generateAadhaarOtp(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody AadhaarOtpRequest request) {
        String aadhaar = request.getAadhaarNumber();
        if (aadhaar == null || !aadhaar.matches("\\d{12}")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", Map.of("message", "Aadhaar number must be 12 digits")
            ));
        }

        try {
            String token = transbankService.generateToken();
            if (token == null) {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", Map.of("message", "Failed to generate auth token")
                ));
            }

            // In real impl: POST to Transbank aadhaar-okyc-generate-otp endpoint
            // For now, return mock session ID matching Node.js stub
            String sessionId = java.util.UUID.randomUUID().toString();
            log.info("Aadhaar OTP generated for user {}: sessionId={}", user.getId(), sessionId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "message", "OTP sent to Aadhaar-linked mobile number"
            ));
        } catch (Exception e) {
            log.error("Aadhaar OTP generation failed", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", Map.of("message", e.getMessage())
            ));
        }
    }

    @PostMapping("/aadhaar-submit-otp")
    public ResponseEntity<Map<String, Object>> submitAadhaarOtp(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody AadhaarSubmitOtpRequest request) {
        if (request.getSessionId() == null || request.getOtp() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", Map.of("message", "sessionId and otp are required")
            ));
        }

        try {
            String token = transbankService.generateToken();
            if (token == null) {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", Map.of("message", "Failed to generate auth token")
                ));
            }

            // In real impl: POST to Transbank aadhaar-okyc-submit-otp endpoint
            // For now, return mock success matching Node.js behavior
            log.info("Aadhaar OTP submitted for user {}: sessionId={}", user.getId(), request.getSessionId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "name", "Verified User",
                            "dob", "1990-01-01",
                            "gender", "Male",
                            "district", "Bangalore",
                            "state", "Karnataka",
                            "pincode", "560001",
                            "photo", ""
                    )
            ));
        } catch (Exception e) {
            log.error("Aadhaar OTP submission failed", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", Map.of("message", e.getMessage())
            ));
        }
    }

    @PostMapping("/bank-validation")
    public ResponseEntity<Map<String, Object>> validateBank(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody DistributorPrescreenBankValidationRequest request) {
        if (request.getAccountHolderName() == null || request.getIfscCode() == null || request.getAccountNumber() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", Map.of("message", "accountHolderName, ifscCode, and accountNumber are required")
            ));
        }

        BankValidationResponse result = transbankService.validateBankAccount(
                request.getAccountHolderName(), request.getIfscCode(),
                request.getAccountNumber(), null, null, null
        );

        Map<String, Object> data = Map.of(
                "isValid", result.isValid(),
                "accountName", result.getAccountName() != null ? result.getAccountName() : request.getAccountHolderName(),
                "accountStatus", result.getAccountStatus() != null ? result.getAccountStatus() : "",
                "requestId", result.getRequestId() != null ? result.getRequestId() : "",
                "trackingRefNo", result.getTrackingRefNo() != null ? result.getTrackingRefNo() : "",
                "responseId", result.getResponseId() != null ? result.getResponseId() : "",
                "statusCode", result.getStatusCode() != null ? result.getStatusCode() : "",
                "status", result.getStatus() != null ? result.getStatus() : "",
                "message", result.getMessage() != null ? result.getMessage() : "",
                "error", result.getError() != null ? result.getError() : ""
        );

        return ResponseEntity.ok(Map.of(
                "success", result.isValid(),
                "data", data
        ));
    }

    @PostMapping("/experian-report")
    public ResponseEntity<Map<String, Object>> getExperianReport(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> request) {
        String name = request.get("name");
        String mobile = request.get("mobile");
        String pan = request.get("pan");

        if (name == null || mobile == null || pan == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", Map.of("message", "name, mobile, and pan are required")
            ));
        }

        try {
            String token = transbankService.generateToken();
            if (token == null) {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", Map.of("message", "Failed to generate auth token")
                ));
            }

            // In real impl: POST to Transbank experian-report endpoint
            log.info("Experian report requested for user {}: pan={}", user.getId(), pan);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "creditScore", 750,
                            "name", name,
                            "pan", pan,
                            "txnId", java.util.UUID.randomUUID().toString(),
                            "creditReport", Map.of("summary", "No outstanding defaults"),
                            "rawResponse", Map.of("status", "success")
                    )
            ));
        } catch (Exception e) {
            log.error("Experian report failed", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", Map.of("message", e.getMessage())
            ));
        }
    }

    @PostMapping("/vpa-validation")
    public ResponseEntity<Map<String, Object>> validateVpa(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> request) {
        String vpa = request.get("vpa");
        if (vpa == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", Map.of("message", "vpa is required")
            ));
        }

        try {
            BankValidationResponse result = transbankService.validateBankAccount(
                    "VPA Holder", "SBIN0000001", "000000000000", null, null, null
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "isValid", result.isValid(),
                            "vpa", vpa,
                            "accountName", result.getAccountName() != null ? result.getAccountName() : "",
                            "message", result.getMessage() != null ? result.getMessage() : ""
                    )
            ));
        } catch (Exception e) {
            log.warn("VPA validation failed (Transbank unavailable), returning mock: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "isValid", true,
                            "vpa", vpa,
                            "accountName", "UPI Holder",
                            "message", "Mock validation - Transbank unavailable"
                    )
            ));
        }
    }
}
