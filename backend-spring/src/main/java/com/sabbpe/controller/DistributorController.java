package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.model.MerchantProfileEntity;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.DistributorService;
import com.sabbpe.service.MerchantService;
import com.sabbpe.service.TransactionService;
import com.sabbpe.service.TransbankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/distributor")
@RequiredArgsConstructor
public class DistributorController {

    private final DistributorService distributorService;
    private final TransbankService transbankService;
    private final MerchantService merchantService;
    private final TransactionService transactionService;

    @PostMapping("/create-merchant")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createMerchant(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateMerchantRequest request) {
        Map<String, Object> result = distributorService.createMerchant(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Merchant created", result));
    }

    @GetMapping("/merchants")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<MerchantProfileResponse>>> getMerchants(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String status) {
        boolean isAdmin = user.getRoles().contains("admin");
        List<MerchantProfileResponse> merchants;
        if (isAdmin) {
            merchants = distributorService.getAllMerchants(status);
        } else {
            merchants = distributorService.getDistributorMerchants(user.getId(), status);
        }
        return ResponseEntity.ok(ApiResponse.success(merchants));
    }

    @PostMapping("/save-bank-details")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> saveBankDetails(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SaveBankDetailsRequest request) {
        distributorService.saveBankDetails(user.getId(), request.getMerchantProfileId(),
                request.getAccountNumber(), request.getIfscCode(), request.getBankName(), request.getAccountHolderName());
        return ResponseEntity.ok(ApiResponse.success("Bank details saved", null));
    }

    @PostMapping("/submit-merchant-onboarding")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> submitMerchant(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody SubmitMerchantOnboardingRequest request) {
        distributorService.submitMerchantOnboarding(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Merchant onboarding submitted successfully", null));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String merchant_id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date_from,
            @RequestParam(required = false) String date_to,
            @RequestParam(defaultValue = "false") boolean all) {
        boolean includeAll = all && user.getRoles().contains("admin");
        Map<String, Object> result = distributorService.getDistributorTransactions(
                user.getId(), includeAll, status, search, merchant_id, type, date_from, date_to);
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/earnings")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getEarnings(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "false") boolean all) {
        boolean includeAll = all && user.getRoles().contains("admin");
        Map<String, Object> result = distributorService.getEarnings(user.getId(), includeAll);
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-vpa")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN', 'MERCHANT')")
    public ResponseEntity<ApiResponse<BankValidationResponse>> validateVpa(
            @RequestParam String vpa) {
        BankValidationResponse result = distributorService.validateVpa(vpa);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/delete-merchant")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMerchant(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String merchantId) {
        distributorService.deleteMerchant(user.getId(), merchantId);
        return ResponseEntity.ok(ApiResponse.success("Merchant deleted", null));
    }

    @GetMapping("/merchant/{id}/settlement-config")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<SettlementConfigResponse>> getSettlementConfig(
            @PathVariable String id) {
        SettlementConfigResponse config = distributorService.getSettlementConfig(id);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PatchMapping("/merchant/{id}/settlement-config")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<SettlementConfigResponse>> updateSettlementConfig(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String id,
            @RequestBody SettlementConfigRequest request) {
        SettlementConfigResponse config = distributorService.updateSettlementConfig(id, request, user.getId(), false);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PatchMapping("/merchant/{id}/settlement-config/admin-override")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SettlementConfigResponse>> adminOverrideSettlementConfig(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable String id,
            @RequestBody SettlementConfigRequest request) {
        SettlementConfigResponse config = distributorService.updateSettlementConfig(id, request, admin.getId(), true);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createDistributor(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateDistributorRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FORBIDDEN", "Not authenticated"));
        }
        if (!user.getRoles().contains("admin")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FORBIDDEN", "Only admins can perform this action. Your roles: " + String.join(", ", user.getRoles())));
        }
        Map<String, Object> result = distributorService.createDistributor(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Distributor created successfully", result));
    }
}
