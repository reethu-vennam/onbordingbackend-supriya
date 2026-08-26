package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.BankValidationRequest;
import com.sabbpe.dto.BankValidationResponse;
import com.sabbpe.dto.OcrResult;
import com.sabbpe.service.OcrService;
import com.sabbpe.service.TransbankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TransbankController {

    private final TransbankService transbankService;
    private final OcrService ocrService;

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

    @PostMapping(value = "/api/kyc/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OcrResult>> documentOcr(
            @RequestPart("doc_front_image") MultipartFile file,
            @RequestParam("doc_type") String docType) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BAD_REQUEST", "No file provided"));
        }
        try {
            OcrResult result = transbankService.documentOcr(file, docType);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("OCR_FAILED", e.getMessage()));
        }
    }
}
