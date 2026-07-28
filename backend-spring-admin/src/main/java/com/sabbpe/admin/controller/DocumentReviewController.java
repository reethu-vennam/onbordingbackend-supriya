package com.sabbpe.admin.controller;

import com.sabbpe.admin.service.DocumentReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/document-review")
@RequiredArgsConstructor
public class DocumentReviewController {

    private final DocumentReviewService documentReviewService;

    @GetMapping("/merchants")
    public ResponseEntity<?> getMerchants() {
        return ResponseEntity.ok(documentReviewService.getMerchantsForReview());
    }

    @GetMapping("/merchants/{merchantId}/score")
    public ResponseEntity<?> computeScore(@PathVariable String merchantId) {
        try {
            return ResponseEntity.ok(documentReviewService.computeScore(merchantId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/documents/{docId}/validate")
    public ResponseEntity<?> validateDocument(@PathVariable String docId) {
        try {
            return ResponseEntity.ok(documentReviewService.validateDocument(docId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/documents/{docId}/approve")
    public ResponseEntity<?> approveDocument(@PathVariable String docId, @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            String staffUserId = "admin"; // from auth context
            return ResponseEntity.ok(documentReviewService.approveDocument(docId, reason, staffUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/documents/{docId}/reject")
    public ResponseEntity<?> rejectDocument(@PathVariable String docId, @RequestBody Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            String staffUserId = "admin"; // from auth context
            return ResponseEntity.ok(documentReviewService.rejectDocument(docId, reason, staffUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/merchants/{merchantId}/experian-check")
    public ResponseEntity<?> runExperianCheck(@PathVariable String merchantId, @RequestParam(required = false) String staffUserId) {
        try {
            return ResponseEntity.ok(documentReviewService.experianCheck(merchantId,
                    staffUserId != null ? staffUserId : "system"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/merchants/{merchantId}/experian-check")
    public ResponseEntity<?> getExperianResult(@PathVariable String merchantId) {
        return ResponseEntity.ok(documentReviewService.getExperianResult(merchantId));
    }
}
