package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.TransactionWebhookRequest;
import com.sabbpe.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final TransactionService transactionService;

    @PostMapping("/sabbpe/callback")
    public ResponseEntity<ApiResponse<Void>> handlePaymentCallback(@RequestBody Map<String, Object> payload) {
        log.info("Received payment callback: {}", payload);

        String txnId = transactionService.extractTransactionIdFromPayload(payload);
        if (txnId != null) {
            try {
                String txnDetails = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
                transactionService.storeTxnDetails(txnId, txnDetails);
            } catch (Exception e) {
                log.error("Failed to store txn details from callback", e);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Callback received", null));
    }
}
