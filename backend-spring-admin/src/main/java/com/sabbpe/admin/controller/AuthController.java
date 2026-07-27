package com.sabbpe.admin.controller;

import com.sabbpe.admin.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = authService.register(
                    request.get("email"), request.get("password"),
                    request.get("fullName"), request.get("role"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = authService.login(request.get("email"), request.get("password"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/support-users")
    public ResponseEntity<?> getSupportUsers() {
        try {
            List<Map<String, Object>> users = authService.getSupportUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/create-support")
    public ResponseEntity<?> createSupportUser(@RequestBody Map<String, String> request) {
        try {
            authService.createSupportUser(request.get("name"), request.get("email"), request.get("password"));
            return ResponseEntity.ok(Map.of("message", "Support created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/support-toggle")
    public ResponseEntity<?> toggleSupportStatus(@RequestBody Map<String, String> request) {
        try {
            authService.toggleSupportStatus(request.get("userId"));
            return ResponseEntity.ok(Map.of("message", "Support status updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/support-delete")
    public ResponseEntity<?> deleteSupportUser(@RequestBody Map<String, String> request) {
        try {
            authService.deleteSupportUser(request.get("userId"));
            return ResponseEntity.ok(Map.of("message", "Support deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
