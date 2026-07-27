package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.BulkInviteRequest;
import com.sabbpe.dto.BulkInviteResponse;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.service.InviteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN', 'EMPLOYEE')")
public class InviteController {

    private final InviteService inviteService;

    @PostMapping("/bulk-send")
    public ResponseEntity<ApiResponse<BulkInviteResponse>> bulkSendInvites(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody BulkInviteRequest request) {
        BulkInviteResponse response = inviteService.bulkSend(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
