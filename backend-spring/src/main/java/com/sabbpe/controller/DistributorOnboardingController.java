package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.AuthService;
import com.sabbpe.service.DistributorOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/distributor/onboarding")
@RequiredArgsConstructor
public class DistributorOnboardingController {

    private final DistributorOnboardingService onboardingService;
    private final AuthService authService;

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("distributorOnboarding router is alive"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listDistributors() {
        var distributors = onboardingService.listDistributors();
        return ResponseEntity.ok(ApiResponse.success(distributors));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DistributorOnboardingStatusResponse>> getStatus(
            @AuthenticationPrincipal CustomUserDetails user) {
        DistributorOnboardingStatusResponse status = onboardingService.getStatus(user.getId());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping("/agreement/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendAgreement(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestBody SendAgreementRequest request) {
        onboardingService.sendAgreement(admin.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Agreement sent successfully", null));
    }

    @GetMapping("/agreement/download")
    public ResponseEntity<ApiResponse<AgreementDownloadResponse>> downloadAgreement(
            @RequestParam String token) {
        AgreementDownloadResponse response = onboardingService.getAgreementByToken(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/agreement/upload-signed")
    public ResponseEntity<ApiResponse<Void>> uploadSignedAgreement(
            @RequestParam String token,
            @RequestBody UploadSignedAgreementRequest request) {
        onboardingService.uploadSignedAgreement(token, request);
        return ResponseEntity.ok(ApiResponse.success("Signed agreement uploaded successfully", null));
    }

    @GetMapping("/signed/{distributorId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSignedAgreement(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String distributorId) {
        boolean isAdmin = user.getRoles().contains("admin");
        Map<String, Object> result = onboardingService.getSignedAgreementUrl(user.getId(), distributorId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveAgreement(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestParam(required = false) String distributorId,
            @RequestBody(required = false) Map<String, Object> body) {
        distributorId = resolveBodyString(distributorId, body, "distributorId");
        onboardingService.approveAgreement(distributorId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Agreement approved successfully", null));
    }

    @PostMapping("/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectAgreement(
            @RequestParam(required = false) String distributorId,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, Object> body) {
        distributorId = resolveBodyString(distributorId, body, "distributorId");
        reason = resolveBodyString(reason, body, "reason");
        onboardingService.rejectAgreement(distributorId, reason);
        return ResponseEntity.ok(ApiResponse.success("Agreement rejected", null));
    }

    @PostMapping("/credentials/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendCredentials(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestParam(required = false) String distributorId,
            @RequestParam(required = false) String password,
            @RequestBody(required = false) Map<String, Object> body) {
        distributorId = resolveBodyString(distributorId, body, "distributorId");
        password = resolveBodyString(password, body, "password");
        Map<String, Object> result = onboardingService.sendCredentials(distributorId, password, admin.getId());

        // Update the user's password in auth using the userId from the profile
        try {
            String userId = (String) result.get("userId");
            if (userId != null) {
                authService.updatePassword(userId, password);
            }
        } catch (Exception e) {
            // Non-fatal - credentials email was already sent with the password
        }

        return ResponseEntity.ok(ApiResponse.success("Credentials sent successfully", result));
    }

    @PostMapping("/kyc/submit")
    public ResponseEntity<ApiResponse<List<String>>> submitKyc(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody DistributorKycRequest request) {
        List<String> missingFields = onboardingService.submitKyc(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("KYC submitted for review", missingFields));
    }

    @PostMapping("/kyc/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveKyc(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestParam(required = false) String distributorId,
            @RequestBody(required = false) Map<String, Object> body) {
        distributorId = resolveBodyString(distributorId, body, "distributorId");
        onboardingService.approveKyc(distributorId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("KYC approved successfully", null));
    }

    @PostMapping("/kyc/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectKyc(
            @RequestParam(required = false) String distributorId,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, Object> body) {
        distributorId = resolveBodyString(distributorId, body, "distributorId");
        reason = resolveBodyString(reason, body, "reason");
        onboardingService.rejectKyc(distributorId, reason, null);
        return ResponseEntity.ok(ApiResponse.success("KYC rejected", null));
    }

    private String resolveBodyString(String value, Map<String, Object> body, String key) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        Object bodyValue = body != null ? body.get(key) : null;
        return bodyValue != null ? String.valueOf(bodyValue) : value;
    }
}
