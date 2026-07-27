package com.sabbpe.service;

import com.sabbpe.dto.BulkInviteRequest;
import com.sabbpe.dto.BulkInviteResponse;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.model.MerchantInvitationEntity;
import com.sabbpe.repository.MerchantInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

    private final MerchantInvitationRepository invitationRepository;
    private final SmsService smsService;
    private final NotificationService notificationService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public BulkInviteResponse bulkSend(String distributorId, BulkInviteRequest request) {
        List<BulkInviteRequest.MerchantInvite> merchants = request.getMerchants();

        if (merchants == null || merchants.isEmpty()) {
            throw new BadRequestException("No merchants provided");
        }
        if (merchants.size() > 10) {
            throw new BadRequestException("Maximum 10 invitations at a time");
        }

        int sent = 0;
        int failed = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        for (BulkInviteRequest.MerchantInvite merchant : merchants) {
            Map<String, Object> result = new HashMap<>();
            result.put("email", merchant.getEmail());
            result.put("fullName", merchant.getFullName());

            try {
                // Validate required fields
                if (merchant.getEmail() == null || merchant.getFullName() == null || merchant.getMobileNumber() == null) {
                    throw new BadRequestException("email, fullName, and mobileNumber are required for each merchant");
                }

                // Format mobile number
                String mobile = merchant.getMobileNumber().trim();
                if (mobile.length() == 10) {
                    mobile = "91" + mobile;
                } else if (!mobile.startsWith("91")) {
                    mobile = "91" + mobile;
                }
                if (mobile.length() < 10 || mobile.length() > 15) {
                    throw new BadRequestException("Invalid mobile number: " + merchant.getMobileNumber());
                }

                // Generate invite token
                String token = UUID.randomUUID().toString();
                String inviteLink = frontendUrl + "/invite/" + token;

                // Create invitation record
                MerchantInvitationEntity invitation = new MerchantInvitationEntity();
                invitation.setDistributorId(distributorId);
                invitation.setMerchantEmail(merchant.getEmail());
                invitation.setMerchantName(merchant.getFullName());
                invitation.setMerchantMobile(mobile);
                invitation.setBusinessName(merchant.getBusinessName());
                invitation.setInvitationToken(token);
                invitation.setInviteLink(inviteLink);
                invitation.setStatus("pending");
                invitation.setSentVia("sms");
                invitation.setSentAt(LocalDateTime.now());
                invitation = invitationRepository.save(invitation);

                // Send SMS via MSG91
                Map<String, Object> smsResult = smsService.sendMerchantInvite(
                        mobile, inviteLink, "SabbPe Distributor", merchant.getFullName()
                );

                if (Boolean.TRUE.equals(smsResult.get("success"))) {
                    invitation.setStatus("sent");
                    invitation.setSmsMessageId((String) smsResult.get("messageId"));
                    sent++;
                    result.put("status", "sent");
                } else {
                    invitation.setStatus("failed_to_send");
                    invitation.setSendError((String) smsResult.get("error"));
                    failed++;
                    result.put("status", "failed");
                    result.put("error", smsResult.get("error"));
                }

                invitationRepository.save(invitation);

                // Send email (non-fatal)
                try {
                    notificationService.sendMerchantInviteEmail(
                            merchant.getEmail(), merchant.getFullName(),
                            inviteLink, "SabbPe Distributor"
                    );
                } catch (Exception e) {
                    log.warn("Failed to send invite email to {}: {}", merchant.getEmail(), e.getMessage());
                }

            } catch (Exception e) {
                failed++;
                result.put("status", "failed");
                result.put("error", e.getMessage());
                log.error("Failed to process invite for {}: {}", merchant.getEmail(), e.getMessage());
            }

            results.add(result);
        }

        return BulkInviteResponse.builder()
                .sent(sent)
                .failed(failed)
                .total(merchants.size())
                .results(results)
                .build();
    }
}
