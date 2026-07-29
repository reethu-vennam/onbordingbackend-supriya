package com.sabbpe.controller;

import com.sabbpe.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @PostMapping("/sabbpe/callback")
    public ResponseEntity<?> handlePaymentCallback(@RequestBody Map<String, Object> payload) {
        log.info("Received SabbPe payment callback");

        String txnId = transactionService.extractTransactionIdFromPayload(payload);
        if (txnId == null || txnId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Transaction ID is required in the callback payload"
            ));
        }

        try {
            String txnDetails = objectMapper.writeValueAsString(payload);
            Map<String, Object> result = transactionService.storeTxnDetails(txnId, txnDetails);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Callback acknowledged");
            response.put("data", Map.of(
                    "transactionId", result.get("transactionId"),
                    "outcome", result.get("outcome")
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to store txn details from callback", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error processing callback"
            ));
        }
    }
}
