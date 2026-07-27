package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/store-transaction-id")
    public ResponseEntity<ApiResponse<Void>> storeTransactionId(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StoreTransactionIdRequest request) {
        transactionService.storeTransactionId(user.getId(), request.getTransactionId());
        return ResponseEntity.ok(ApiResponse.success("Transaction ID stored", null));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Void>> updateTransaction(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StoreTransactionIdRequest request) {
        transactionService.storeTransactionId(user.getId(), request.getTransactionId());
        return ResponseEntity.ok(ApiResponse.success("Transaction updated", null));
    }

    @PostMapping("/store-txn-details")
    public ResponseEntity<ApiResponse<Void>> storeTxnDetails(
            @RequestBody StoreTxnDetailsRequest request) {
        transactionService.storeTxnDetails(request.getTransactionId(), request.getTxnDetails());
        return ResponseEntity.ok(ApiResponse.success("Transaction details stored", null));
    }

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<String>> getTransactionDetails(
            @AuthenticationPrincipal CustomUserDetails user) {
        String txnId = transactionService.getTransactionId(user.getId());
        return ResponseEntity.ok(ApiResponse.success(txnId));
    }

    @GetMapping("/get")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @RequestParam String transactionId) {
        TransactionResponse txn = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success(txn));
    }
}
