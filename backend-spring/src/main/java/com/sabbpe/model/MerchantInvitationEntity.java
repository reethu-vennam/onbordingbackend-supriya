package com.sabbpe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInvitationEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "distributor_id", nullable = false, length = 36)
    private String distributorId;

    @Column(name = "merchant_email", nullable = false)
    private String merchantEmail;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "merchant_mobile", nullable = false, length = 20)
    private String merchantMobile;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "invitation_token", nullable = false, length = 255)
    private String invitationToken;

    @Column(name = "invite_link", columnDefinition = "TEXT")
    private String inviteLink;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "pending";

    @Column(name = "sent_via", length = 20)
    private String sentVia;

    @Column(name = "sms_message_id", length = 255)
    private String smsMessageId;

    @Column(name = "send_error", columnDefinition = "TEXT")
    private String sendError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusDays(14);
    }
}
