package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.ChargebackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chargeback")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
public class ChargebackController {

    private final ChargebackService chargebackService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ChargebackResponse>> createChargeback(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CreateChargebackRequest request) {
        ChargebackResponse response = chargebackService.createChargeback(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Chargeback created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChargebackResponse>>> getChargebacks(
            @RequestParam(required = false) String merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        List<ChargebackResponse> chargebacks = chargebackService.getChargebacks(merchantId, Math.max(0, page - 1), limit, status);
        return ResponseEntity.ok(ApiResponse.success(chargebacks));
    }

    @GetMapping("/{chargebackId}")
    public ResponseEntity<ApiResponse<ChargebackResponse>> getChargeback(
            @PathVariable String chargebackId) {
        ChargebackResponse response = chargebackService.getChargebackResponse(chargebackId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/recover")
    public ResponseEntity<ApiResponse<ChargebackResponse>> recoverChargeback(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String chargebackId) {
        ChargebackResponse response = chargebackService.recoverChargeback(chargebackId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Chargeback recovery processed", response));
    }

    @GetMapping("/history/{chargebackId}")
    public ResponseEntity<ApiResponse<List<ChargebackHistoryResponse>>> getHistory(
            @PathVariable String chargebackId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        List<ChargebackHistoryResponse> history = chargebackService.getChargebackHistory(chargebackId, Math.max(0, page - 1), limit);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ChargebackSummaryResponse>> getSummary(
            @RequestParam String merchantId) {
        ChargebackSummaryResponse summary = chargebackService.getChargebackSummary(merchantId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/distributor-recovery-summary")
    public ResponseEntity<ApiResponse<DistributorRecoverySummaryResponse>> getDistributorRecoverySummary(
            @AuthenticationPrincipal CustomUserDetails user) {
        DistributorRecoverySummaryResponse summary = chargebackService.getDistributorRecoverySummary(user.getId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
