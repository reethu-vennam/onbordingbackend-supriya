package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.model.RollingReserveLedgerEntity;
import com.sabbpe.model.SettlementHistoryEntity;
import com.sabbpe.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<SettlementRunResponse>> runBatch(
            @RequestParam(defaultValue = "false") boolean dryRun) {
        SettlementRunResponse result = settlementService.processSettlementBatch(dryRun);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/run/{merchantId}")
    public ResponseEntity<ApiResponse<SettlementRunResponse.MerchantResult>> runForMerchant(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        SettlementRunResponse.MerchantResult result = settlementService.processSettlementForMerchant(merchantId, dryRun);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<SettlementHistoryResponse>>> getHistory(
            @RequestParam String merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        List<SettlementHistoryEntity> history = settlementService.getSettlementHistory(merchantId, Math.max(0, page - 1), limit);
        List<SettlementHistoryResponse> response = history.stream()
                .map(h -> SettlementHistoryResponse.builder()
                        .id(h.getId())
                        .merchantId(h.getMerchantId())
                        .settlementBatchRef(h.getSettlementBatchRef())
                        .settlementDate(h.getSettlementDate())
                        .grossAmount(h.getGrossAmount())
                        .mdrDeduction(h.getMdrDeduction())
                        .rollingReserveHeld(h.getRollingReserveHeld())
                        .netSettlementAmount(h.getNetSettlementAmount())
                        .transactionCount(h.getTransactionCount())
                        .status(h.getStatus())
                        .processedAt(h.getProcessedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history/{merchantId}")
    public ResponseEntity<ApiResponse<List<SettlementHistoryResponse>>> getMerchantHistory(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return getHistory(merchantId, page, limit);
    }

    @GetMapping("/summary/{merchantId}")
    public ResponseEntity<ApiResponse<SettlementSummaryResponse>> getSummary(
            @PathVariable String merchantId) {
        SettlementSummaryResponse summary = settlementService.getSettlementSummary(merchantId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/ledger/{merchantId}")
    public ResponseEntity<ApiResponse<List<ReserveLedgerResponse>>> getLedger(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        List<RollingReserveLedgerEntity> ledger = settlementService.getReserveLedger(merchantId, Math.max(0, page - 1), limit);
        List<ReserveLedgerResponse> response = ledger.stream()
                .map(e -> ReserveLedgerResponse.builder()
                        .id(e.getId())
                        .transactionRef(e.getTransactionRef())
                        .reserveAmount(e.getReserveAmount())
                        .reserveDate(e.getReserveDate())
                        .releaseDate(e.getReleaseDate())
                        .status(e.getStatus())
                        .releasedAt(e.getReleasedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/preview/{merchantId}")
    public ResponseEntity<ApiResponse<SettlementPreviewResponse>> preview(
            @PathVariable String merchantId) {
        SettlementPreviewResponse preview = settlementService.previewSettlement(merchantId);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    @PostMapping("/release-reserves")
    public ResponseEntity<ApiResponse<Integer>> releaseReserves() {
        int released = settlementService.releaseReserve();
        return ResponseEntity.ok(ApiResponse.success("Released " + released + " reserve entries", released));
    }
}
