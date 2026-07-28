package com.sabbpe.admin.controller;

import com.sabbpe.admin.exception.ResourceNotFoundException;
import com.sabbpe.admin.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody Map<String, String> request, Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            Map<String, Object> ticket = ticketService.createTicket(
                    request.get("module"), request.get("referenceId"),
                    request.get("title"), request.get("description"),
                    request.get("priority"), userId);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getTickets(Authentication auth,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int limit,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String module) {
        try {
            String userId = (String) auth.getPrincipal();
            String role = getRole(auth);
            Map<String, Object> result = ticketService.getTickets(userId, role, status, module, page, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assignTicket(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = ticketService.assignTicket(request.get("ticketId"), request.get("userId"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestBody Map<String, String> request, Authentication auth) {
        try {
            String role = getRole(auth);
            Map<String, Object> result = ticketService.updateTicketStatus(request.get("ticketId"), request.get("status"), role);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            return ResponseEntity.ok(ticketService.getTicketStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/merchant-review")
    public ResponseEntity<?> reviewMerchant(@RequestBody Map<String, String> request, Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            String merchantId = request.get("merchantId") != null ? request.get("merchantId") : request.get("merchant_id");
            String status = request.get("status");
            String reviewNotes = request.get("reviewNotes") != null ? request.get("reviewNotes") : request.get("review_notes");
            Map<String, Object> result = ticketService.reviewMerchant(merchantId, status, reviewNotes, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/merchant-review/{merchantId}")
    public ResponseEntity<?> getMerchantReviewData(@PathVariable String merchantId, Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            String role = getRole(auth);
            Map<String, Object> result = ticketService.getMerchantReviewData(merchantId, role, userId);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("getMerchantReviewData failed for merchantId={}: {}", merchantId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/kyc-list")
    public ResponseEntity<?> getKycList() {
        try {
            return ResponseEntity.ok(ticketService.getKycList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/support/assigned-kyc")
    public ResponseEntity<?> getAssignedKyc(Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            return ResponseEntity.ok(ticketService.getAssignedKycForSupport(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verify-cpv")
    public ResponseEntity<?> verifyCpv(@RequestBody Map<String, String> request, Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            String merchantId = request.get("merchantId") != null ? request.get("merchantId") : request.get("merchant_id");
            Map<String, Object> result = ticketService.verifyCpv(merchantId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reject-cpv")
    public ResponseEntity<?> rejectCpv(@RequestBody Map<String, String> request) {
        try {
            String merchantId = request.get("merchantId") != null ? request.get("merchantId") : request.get("merchant_id");
            String reason = request.get("reason") != null ? request.get("reason") : request.get("review_notes");
            Map<String, Object> result = ticketService.rejectCpv(merchantId, reason);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/all-documents")
    public ResponseEntity<?> getAllDocuments(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String document_type) {
        try {
            return ResponseEntity.ok(ticketService.getAllDocuments(status, document_type));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/review-document")
    public ResponseEntity<?> reviewDocument(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = ticketService.reviewDocument(
                    request.get("documentId"), request.get("status"), request.get("rejectionReason"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/merchant")
    public ResponseEntity<?> createMerchantTicket(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> ticket = ticketService.createMerchantTicket(
                    request.get("merchantId"), request.get("title"), request.get("description"));
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<?> getMerchantTickets(@PathVariable String merchantId) {
        try {
            return ResponseEntity.ok(ticketService.getMerchantTickets(merchantId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/merchant/{ticketId}/messages")
    public ResponseEntity<?> getMerchantMessages(@PathVariable String ticketId,
                                                 @RequestParam String merchant_id) {
        try {
            return ResponseEntity.ok(ticketService.getMerchantTicketMessages(ticketId, merchant_id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/merchant/{ticketId}/messages")
    public ResponseEntity<?> sendMerchantMessage(@PathVariable String ticketId,
                                                 @RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = ticketService.sendMerchantTicketMessage(
                    ticketId, request.get("merchantId"), request.get("message"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{ticketId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String ticketId) {
        try {
            return ResponseEntity.ok(ticketService.getTicketMessages(ticketId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{ticketId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable String ticketId,
                                         @RequestBody Map<String, String> request,
                                         Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            String role = getRole(auth);
            Map<String, Object> result = ticketService.sendTicketMessage(
                    ticketId, userId, role, request.get("message"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private String getRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("support");
    }
}
