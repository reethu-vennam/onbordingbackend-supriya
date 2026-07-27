package com.sabbpe.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/bank/auth")
public class BankAuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        log.info("Bank staff login attempt: {}", email);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful",
                "token", "bank_" + UUID.randomUUID(),
                "user", Map.of(
                        "userId", UUID.randomUUID().toString(),
                        "email", email,
                        "role", "bank_staff",
                        "name", email,
                        "bankStaffId", "BS_" + email.hashCode()
                )
        ));
    }
}
