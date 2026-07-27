package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.LoginRequest;
import com.sabbpe.model.MerchantKycEntity;
import com.sabbpe.model.MerchantProfileEntity;
import com.sabbpe.repository.MerchantKycRepository;
import com.sabbpe.repository.MerchantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantKycRepository kycRepository;

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.error("NOT_IMPLEMENTED", "Support staff login not yet implemented"));
    }

    @GetMapping("/kyc/pending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingKyc() {
        List<MerchantKycEntity> pendingKyc = kycRepository.findAll().stream()
                .filter(k -> "submitted".equals(k.getKycStatus()) || "pending".equals(k.getKycStatus()))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = pendingKyc.stream()
                .map(k -> {
                    MerchantProfileEntity merchant = merchantProfileRepository.findById(k.getMerchantId()).orElse(null);
                    return Map.<String, Object>of(
                            "kycId", k.getId(),
                            "merchantId", k.getMerchantId(),
                            "businessName", merchant != null ? merchant.getBusinessName() : "N/A",
                            "kycStatus", k.getKycStatus(),
                            "videoKycCompleted", k.getVideoKycCompleted()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/kyc/review")
    public ResponseEntity<ApiResponse<Void>> reviewKyc(@RequestBody Map<String, Object> review) {
        String kycId = review.get("kycId") != null ? review.get("kycId").toString() : null;
        String status = review.get("status") != null ? review.get("status").toString() : null;
        String reason = review.get("reason") != null ? review.get("reason").toString() : null;

        log.info("KYC review for {}: status={}, reason={}", kycId, status, reason);
        return ResponseEntity.ok(ApiResponse.success("KYC reviewed", null));
    }

    @GetMapping("/kyc/status/{merchantId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKycStatus(@PathVariable String merchantId) {
        MerchantKycEntity kyc = kycRepository.findByMerchantId(merchantId).orElse(null);
        if (kyc == null) {
            return ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "KYC not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "kycStatus", kyc.getKycStatus(),
                "videoKycCompleted", kyc.getVideoKycCompleted(),
                "locationCaptured", kyc.getLocationCaptured()
        )));
    }
}
