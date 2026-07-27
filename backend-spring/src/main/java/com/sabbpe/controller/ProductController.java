package com.sabbpe.controller;

import com.sabbpe.dto.*;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.MerchantProductService;
import com.sabbpe.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final MerchantProductService merchantProductService;

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getCatalog() {
        return ResponseEntity.ok(ApiResponse.success(productService.getCatalog()));
    }

    @GetMapping("/sub-catalog/{parentProductCode}")
    public ResponseEntity<ApiResponse<List<SubProductResponse>>> getSubCatalog(
            @PathVariable String parentProductCode) {
        return ResponseEntity.ok(ApiResponse.success(productService.getSubCatalog(parentProductCode)));
    }

    @PostMapping("/merchant/update-products")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> updateProducts(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateProductsRequest request) {
        MerchantProfileResponse response = merchantProductService.updateProducts(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/merchant/selected-products")
    public ResponseEntity<ApiResponse<String>> getSelectedProducts(
            @AuthenticationPrincipal CustomUserDetails user) {
        String products = merchantProductService.getSelectedProducts(user.getId());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/merchant/selected-sub-products/{parentProductCode}")
    public ResponseEntity<ApiResponse<List<SubProductResponse>>> getSelectedSubProducts(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String parentProductCode) {
        List<SubProductResponse> subs = merchantProductService.getSelectedSubProducts(user.getId(), parentProductCode);
        return ResponseEntity.ok(ApiResponse.success(subs));
    }

    @PostMapping("/merchant/update-sub-products")
    public ResponseEntity<ApiResponse<MerchantProfileResponse>> updateSubProducts(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateProductsRequest request) {
        MerchantProfileResponse response = merchantProductService.updateProducts(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/merchant/sign-agreement")
    public ResponseEntity<ApiResponse<AgreementResponse>> signAgreement(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody SignAgreementRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        AgreementResponse agreement = merchantProductService.signAgreement(user.getId(), request, ip);
        return ResponseEntity.ok(ApiResponse.success("Agreement signed", agreement));
    }

    @GetMapping("/merchant/agreements")
    public ResponseEntity<ApiResponse<List<AgreementResponse>>> getAgreements(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<AgreementResponse> agreements = merchantProductService.getAgreements(user.getId());
        return ResponseEntity.ok(ApiResponse.success(agreements));
    }
}
