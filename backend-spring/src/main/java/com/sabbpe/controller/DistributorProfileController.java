package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.DistributorProfileRequest;
import com.sabbpe.dto.DistributorProfileResponse;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.DistributorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/distributor/profile")
@RequiredArgsConstructor
public class DistributorProfileController {

    private final DistributorProfileService distributorProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<DistributorProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails user) {
        DistributorProfileResponse profile = distributorProfileService.getProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<DistributorProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody DistributorProfileRequest request) {
        DistributorProfileResponse profile = distributorProfileService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
