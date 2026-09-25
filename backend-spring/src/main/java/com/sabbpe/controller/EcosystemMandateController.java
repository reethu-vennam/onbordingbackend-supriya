package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.EcosystemMandateCreateRequest;
import com.sabbpe.dto.EcosystemMandateCreateResponse;
import com.sabbpe.dto.EcosystemMandateStatusResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.sabbpe.exception.ResourceNotFoundException;
import com.sabbpe.model.MerchantProfileEntity;
import com.sabbpe.repository.MerchantProfileRepository;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.SabbpeEcosystemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Fronts the Onboarding Team's 5 ecosystem APIs for the frontend. See
 * SabbpeEcosystemService
 * for the actual onboard -> token -> mandate -> status -> subscription
 * orchestration; this
 * controller only resolves the calling merchant and delegates.
 */
@RestController
@RequestMapping("/api/merchant/ecosystem")
@RequiredArgsConstructor
public class EcosystemMandateController {

    private final SabbpeEcosystemService ecosystemService;
    private final MerchantProfileRepository merchantProfileRepository;

    @PostMapping("/vpa/validate")
    public ResponseEntity<ApiResponse<JsonNode>> validateVpa(@RequestBody Map<String, String> request) {
        String vpa = request.get("vpa");
        if (vpa == null || vpa.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "vpa is required"));
        }
        return ResponseEntity.ok(ApiResponse.success(ecosystemService.validateVpa(vpa.trim())));
    }
    // TEsting

    @PostMapping("/mandate/create")
    public ResponseEntity<ApiResponse<EcosystemMandateCreateResponse>> createMandate(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody EcosystemMandateCreateRequest request) {
        MerchantProfileEntity merchant = getMerchant(user.getId());
        EcosystemMandateCreateResponse response = ecosystemService.createUpiMandate(merchant, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mandate/status")
    public ResponseEntity<ApiResponse<EcosystemMandateStatusResponse>> pollMandateStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> request) {
        String trxnno = request.get("trxnno");
        if (trxnno == null || trxnno.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "trxnno is required"));
        }
        MerchantProfileEntity merchant = getMerchant(user.getId());
        EcosystemMandateStatusResponse response = ecosystemService.pollStatus(merchant, trxnno);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private MerchantProfileEntity getMerchant(String userId) {
        return merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));
    }
}
