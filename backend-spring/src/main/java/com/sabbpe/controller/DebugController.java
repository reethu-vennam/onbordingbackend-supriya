package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/token-claims")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenClaims(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error("UNAUTHORIZED", "No authenticated user"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "roles", user.getRoles(),
                "merchantId", user.getMerchantId(),
                "authorities", user.getAuthorities().stream()
                        .map(Object::toString)
                        .toList()
        )));
    }
}
