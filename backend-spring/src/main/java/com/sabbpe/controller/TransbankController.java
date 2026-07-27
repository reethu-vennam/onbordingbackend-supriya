package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.BankValidationRequest;
import com.sabbpe.dto.BankValidationResponse;
import com.sabbpe.service.TransbankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TransbankController {

    private final TransbankService transbankService;

    @PostMapping("/api/v1/token/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateToken() {
        String token = transbankService.generateToken();
        if (token != null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("token", token)));
        }
        return ResponseEntity.status(500)
                .body(ApiResponse.error("TOKEN_ERROR", "Failed to generate token"));
    }

    @PostMapping("/api/bank-account-validation")
    public ResponseEntity<ApiResponse<BankValidationResponse>> validateBankAccount(
            @Valid @RequestBody BankValidationRequest request) {
        BankValidationResponse result = transbankService.validateBankAccount(
                request.getCustName(), request.getCustIfsc(), request.getCustAcctNo(),
                request.getRequestId(), request.getTrackingRefNo(), request.getTxnType());
        if (result.isValid()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        return ResponseEntity.ok(ApiResponse.error("VALIDATION_FAILED", "Bank account validation failed", result));
    }
}
