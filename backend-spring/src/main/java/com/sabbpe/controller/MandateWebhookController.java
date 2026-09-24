package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.service.SabbpeEcosystemService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives the ecosystem's mandate-status webhook (ONBOARDING_INTEGRATION_GUIDE.md §11.4).
 * This is the PRIMARY notification for a mandate going active — our own polling
 * (SabbpeEcosystemService.pollStatus) is only the documented recovery fallback. Public
 * endpoint (see SecurityConfig) since it's called server-to-server by the ecosystem, not by
 * a logged-in user.
 */
@Slf4j
@RestController
@RequestMapping("/api/mandates")
@RequiredArgsConstructor
public class MandateWebhookController {

    private final SabbpeEcosystemService ecosystemService;

    @PostMapping("/status-callback")
    public ResponseEntity<ApiResponse<Void>> statusCallback(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Sabbpe-Signature", required = false) String signature,
            @RequestHeader(value = "X-Sabbpe-Event", required = false) String event,
            HttpServletRequest httpRequest) {
        log.info("Mandate status webhook received (event={}, from={})", event, httpRequest.getRemoteAddr());

        if (!ecosystemService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Mandate status webhook signature verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("INVALID_SIGNATURE", "Signature verification failed"));
        }

        ecosystemService.handleStatusWebhook(rawBody);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
