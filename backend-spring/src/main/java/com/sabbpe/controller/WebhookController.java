package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.TransactionResponse;
import com.sabbpe.dto.TransactionWebhookRequest;
import com.sabbpe.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final TransactionService transactionService;

    @PostMapping("/transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> handleTransactionWebhook(
            @RequestBody TransactionWebhookRequest request) {
        log.info("Received transaction webhook: {}", request.getTransactionId());
        TransactionResponse txn = transactionService.recordTransaction(request);
        if (txn != null) {
            return ResponseEntity.ok(ApiResponse.success("Transaction recorded", txn));
        }
        return ResponseEntity.ok(ApiResponse.success("Duplicate transaction skipped", null));
    }

    @PostMapping("/transaction/test")
    public ResponseEntity<ApiResponse<TransactionResponse>> testTransactionWebhook() {
        TransactionWebhookRequest test = new TransactionWebhookRequest();
        test.setTransactionId("test-" + System.currentTimeMillis());
        test.setAmount(java.math.BigDecimal.valueOf(100));
        test.setStatus("completed");

        TransactionResponse txn = transactionService.recordTransaction(test);
        return ResponseEntity.ok(ApiResponse.success("Test transaction recorded", txn));
    }

}
