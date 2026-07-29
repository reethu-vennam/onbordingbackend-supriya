package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/store-transaction-id")
    public ResponseEntity<?> storeTransactionId(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> body) {

        String transactionId = transactionService.extractTransactionIdFromPayload(body);
        if (transactionId == null) {
            transactionId = (String) body.getOrDefault("transactionId", body.get("transaction_id"));
        }
        if (transactionId == null || transactionId.toString().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "transaction_id is required"));
        }

        String userId = user.getId();
        Map<String, Object> result = transactionService.storeTransactionId(userId, transactionId.toString());

        String message = "stored".equals(result.get("outcome"))
                ? "Transaction ID stored successfully" : "Transaction ID already stored";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateTransaction(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> body) {

        String transactionId = transactionService.extractTransactionIdFromPayload(body);
        if (transactionId == null) {
            transactionId = (String) body.getOrDefault("transactionId", body.get("transaction_id"));
        }
        if (transactionId == null || transactionId.toString().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Transaction ID is required"));
        }

        String userId = user.getId();
        Map<String, Object> result = transactionService.storeTransactionId(userId, transactionId.toString());

        String message = "stored".equals(result.get("outcome"))
                ? "Transaction ID stored successfully" : "Transaction ID already stored";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/store-txn-details")
    public ResponseEntity<?> storeTxnDetails(
            @RequestBody Map<String, Object> body) {

        String transactionId = transactionService.extractTransactionIdFromPayload(body);
        if (transactionId == null || transactionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "transaction_id is required in the decrypt-token response"
            ));
        }

        String txnDetails;
        try {
            txnDetails = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid payload"));
        }

        Map<String, Object> result = transactionService.storeTxnDetails(transactionId, txnDetails);

        if ("not_found".equals(result.get("outcome"))) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Merchant profile not found for the provided transaction id"
            ));
        }

        String message = "stored".equals(result.get("outcome"))
                ? "Transaction details stored successfully" : "Transaction details already stored";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/details")
    public ResponseEntity<?> getTransactionDetails(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> body) {

        String transactionId = transactionService.extractTransactionIdFromPayload(body);
        if (transactionId == null) {
            transactionId = (String) body.getOrDefault("transactionId", body.get("transaction_id"));
        }
        if (transactionId == null || transactionId.toString().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "transaction_id is required"));
        }

        try {
            Map<String, Object> data = transactionService.getTransactionDetails(user.getId(), transactionId.toString());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Merchant profile not found for the provided transaction id"
            ));
        }
    }

    @GetMapping("/get")
    public ResponseEntity<?> getTransaction(
            @AuthenticationPrincipal CustomUserDetails user) {

        Map<String, Object> data = transactionService.getMerchantTransactionId(user.getId());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
