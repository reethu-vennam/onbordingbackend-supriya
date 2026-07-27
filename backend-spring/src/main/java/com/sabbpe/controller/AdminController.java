package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.MerchantProfileResponse;
import com.sabbpe.dto.MerchantStatusRequest;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final MerchantService merchantService;

    @GetMapping("/merchants")
    public ResponseEntity<ApiResponse<List<MerchantProfileResponse>>> getAllMerchants(
            @RequestParam(required = false) String status) {
        List<MerchantProfileResponse> merchants = merchantService.getAllMerchants(status);
        return ResponseEntity.ok(ApiResponse.success(merchants));
    }

    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> getMerchant(
            @PathVariable String merchantId) {
        MerchantProfileResponse merchant = merchantService.getMerchantResponse(merchantId);
        return ResponseEntity.ok(ApiResponse.success(merchant));
    }

    @PostMapping("/merchants/{merchantId}/validate")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> validateMerchant(
            @PathVariable String merchantId,
            @AuthenticationPrincipal CustomUserDetails admin) {
        MerchantProfileResponse result = merchantService.updateStatus(
                merchantId, "validating", null, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Merchant moved to validating", result));
    }

    @PostMapping("/merchants/{merchantId}/submit-to-bank")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> submitToBank(
            @PathVariable String merchantId,
            @AuthenticationPrincipal CustomUserDetails admin) {
        MerchantProfileResponse result = merchantService.updateStatus(
                merchantId, "pending_bank_approval", null, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Merchant submitted to bank", result));
    }

    @PostMapping("/merchants/{merchantId}/approve")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> approveMerchant(
            @PathVariable String merchantId,
            @AuthenticationPrincipal CustomUserDetails admin) {
        MerchantProfileResponse result = merchantService.updateStatus(
                merchantId, "approved", null, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Merchant approved", result));
    }

    @PostMapping("/merchants/{merchantId}/reject")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> rejectMerchant(
            @PathVariable String merchantId,
            @Valid @RequestBody MerchantStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails admin) {
        MerchantProfileResponse result = merchantService.updateStatus(
                merchantId, "rejected", request.getReason(), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Merchant rejected", result));
    }

    @PostMapping("/merchants/{merchantId}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteMerchant(@PathVariable String merchantId) {
        merchantService.deleteMerchant(merchantId);
        return ResponseEntity.ok(ApiResponse.success("Merchant deleted", null));
    }
}
