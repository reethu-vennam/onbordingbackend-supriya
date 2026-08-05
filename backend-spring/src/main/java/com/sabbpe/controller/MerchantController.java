package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.MerchantService;
import com.sabbpe.service.TransbankService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/merchant", "/api/merchants"})
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final TransbankService transbankService;

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> saveProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody MerchantProfileRequest request) {
        MerchantProfileResponse response = merchantService.saveOrUpdateProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails user) {
        MerchantProfileResponse response = merchantService.getMerchantResponseByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> submitProfile(
            @AuthenticationPrincipal CustomUserDetails user) {
        MerchantProfileResponse response = merchantService.submitProfile(user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Profile submitted successfully", response));
    }

    @PostMapping("/validate-bank-account")
    public ResponseEntity<ApiResponse<BankValidationResponse>> validateBankAccount(
            @Valid @RequestBody BankValidationRequest request) {
        BankValidationResponse result = transbankService.validateBankAccount(
                request.getCustName(), request.getCustIfsc(), request.getCustAcctNo(),
                request.getRequestId(), request.getTrackingRefNo(), request.getTxnType());
        if (result.isValid()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        ApiResponse<BankValidationResponse> response = ApiResponse.<BankValidationResponse>builder()
                .success(false)
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/integration-cost")
    public ResponseEntity<ApiResponse<IntegrationCostResponse>> getIntegrationCost(
            @AuthenticationPrincipal CustomUserDetails user) {
        IntegrationCostResponse cost = merchantService.getIntegrationCost(user.getId());
        return ResponseEntity.ok(ApiResponse.success(cost));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MerchantProfileResponse>>> getAllMerchants(
            @RequestParam(required = false) String status) {
        List<MerchantProfileResponse> merchants = merchantService.getAllMerchants(status);
        return ResponseEntity.ok(ApiResponse.success(merchants));
    }

    @GetMapping("/{merchantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> getMerchantById(
            @PathVariable String merchantId) {
        MerchantProfileResponse merchant = merchantService.getMerchantResponse(merchantId);
        return ResponseEntity.ok(ApiResponse.success(merchant));
    }

    @PostMapping("/{merchantId}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> validateMerchant(
            @PathVariable String merchantId) {
        MerchantProfileResponse response = merchantService.updateStatus(merchantId, "validating", null, null);
        return ResponseEntity.ok(ApiResponse.success("Merchant validated", response));
    }

    @PostMapping("/{merchantId}/submit-to-bank")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> submitToBank(
            @PathVariable String merchantId) {
        MerchantProfileResponse response = merchantService.updateStatus(merchantId, "pending_bank_approval", null, null);
        return ResponseEntity.ok(ApiResponse.success("Submitted to bank", response));
    }

    @PostMapping("/{merchantId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> approveMerchant(
            @PathVariable String merchantId) {
        MerchantProfileResponse response = merchantService.updateStatus(merchantId, "approved", null, null);
        return ResponseEntity.ok(ApiResponse.success("Merchant approved", response));
    }

    @PostMapping("/{merchantId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> rejectMerchant(
            @PathVariable String merchantId,
            @RequestBody java.util.Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Rejected by admin");
        MerchantProfileResponse response = merchantService.updateStatus(merchantId, "rejected", reason, null);
        return ResponseEntity.ok(ApiResponse.success("Merchant rejected", response));
    }

    @PostMapping("/restart-onboarding")
    public ResponseEntity<ApiResponse<Void>> restartOnboarding(@AuthenticationPrincipal CustomUserDetails user) {
        merchantService.restartOnboarding(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Onboarding restarted", null));
    }

    @PostMapping("/accept-pg-commercials")
    public ResponseEntity<ApiResponse<Void>> acceptPgCommercials(@AuthenticationPrincipal CustomUserDetails user) {
        merchantService.acceptPgCommercials(user.getId());
        return ResponseEntity.ok(ApiResponse.success("PG commercials accepted", null));
    }

    @PostMapping("/submit-cpv")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitCpv(@AuthenticationPrincipal CustomUserDetails user,
                                                        @RequestBody Map<String, String> request) {
        String cpvVideoPath = request.get("cpv_video_path");
        merchantService.submitCpv(user.getId(), cpvVideoPath);
        return ResponseEntity.ok(ApiResponse.success("CPV submitted", Map.of("success", true)));
    }

    @PostMapping("/sign-pg-agreement")
    public ResponseEntity<ApiResponse<Void>> signPgAgreement(@AuthenticationPrincipal CustomUserDetails user,
                                                              @RequestBody SignAgreementRequest request,
                                                              HttpServletRequest httpRequest) {
        merchantService.signPgAgreement(user.getId(), request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("PG agreement signed", null));
    }

    @PostMapping("/confirm-agreement-signed")
    public ResponseEntity<ApiResponse<Void>> confirmAgreementSigned(@AuthenticationPrincipal CustomUserDetails user) {
        merchantService.confirmAgreementSigned(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Agreement confirmed", null));
    }

    @PostMapping("/save-split-config")
    public ResponseEntity<ApiResponse<Void>> saveSplitConfig(@AuthenticationPrincipal CustomUserDetails user,
                                                              @RequestBody SplitConfigRequest request) {
        merchantService.saveSplitConfig(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Split config saved", null));
    }

    @PostMapping("/mandate-status")
    public ResponseEntity<ApiResponse<Void>> updateMandateStatus(@AuthenticationPrincipal CustomUserDetails user,
                                                                   @RequestBody MandateStatusRequest request) {
        merchantService.updateMandateStatus(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Mandate status updated", null));
    }

    @DeleteMapping("/{merchantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMerchant(@PathVariable String merchantId) {
        merchantService.deleteMerchant(merchantId);
        return ResponseEntity.ok(ApiResponse.success("Merchant deleted successfully", null));
    }
}
