package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.service.MerchantProductService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Server-to-server endpoint for the Merchant Dashboard backend, which has no shared
 * merchant ID with this service — it only knows a client's email/mobile, so that's
 * how the lookup is keyed. Protected by a shared API key (app.internal-api.key)
 * instead of the normal JWT auth, since the caller is a service, not a logged-in user.
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/merchant-products")
@RequiredArgsConstructor
public class InternalMerchantProductController {

    private final MerchantProductService merchantProductService;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @GetMapping("/selected-codes")
    public ResponseEntity<ApiResponse<List<String>>> getSelectedProductCodes(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobile) {

        if (internalApiKey == null || internalApiKey.isBlank() || !internalApiKey.equals(apiKey)) {
            log.warn("Internal merchant-products lookup rejected: invalid or missing API key");
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "Invalid API key"));
        }

        if ((email == null || email.isBlank()) && (mobile == null || mobile.isBlank())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "email or mobile is required"));
        }

        List<String> codes = merchantProductService.getSelectedProductCodesByContact(email, mobile);
        return ResponseEntity.ok(ApiResponse.success(codes));
    }

    /**
     * Called only after the merchant's SabbPe payment for these products has actually
     * succeeded (see the Dashboard's payment-result page) — never when they're merely
     * added to the cart, so a merchant's records never show something they haven't paid for.
     */
    @PostMapping("/add-codes")
    public ResponseEntity<ApiResponse<List<String>>> addProductCodes(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestBody AddProductCodesRequest request) {

        if (internalApiKey == null || internalApiKey.isBlank() || !internalApiKey.equals(apiKey)) {
            log.warn("Internal merchant-products add rejected: invalid or missing API key");
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "Invalid API key"));
        }

        if ((request.getEmail() == null || request.getEmail().isBlank())
                && (request.getMobile() == null || request.getMobile().isBlank())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "email or mobile is required"));
        }
        if (request.getProductCodes() == null || request.getProductCodes().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "productCodes is required"));
        }

        List<String> merged = merchantProductService.addProductCodesByContact(
                request.getEmail(), request.getMobile(), request.getProductCodes());
        return ResponseEntity.ok(ApiResponse.success(merged));
    }

    @Data
    public static class AddProductCodesRequest {
        private String email;
        private String mobile;
        private List<String> productCodes;
    }
}
