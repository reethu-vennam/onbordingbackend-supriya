package com.sabbpe.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_credit_checks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantCreditCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(nullable = false, length = 50)
    private String provider = "experian";

    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "pan_match")
    private Boolean panMatch;

    @Column(name = "name_match")
    private Boolean nameMatch;

    @Column(name = "mobile_match")
    private Boolean mobileMatch;

    @Column(name = "account_number_match")
    private Boolean accountNumberMatch;

    @Column(name = "bank_name_match")
    private Boolean bankNameMatch;

    @Column(name = "has_defaults")
    private Boolean hasDefaults;

    @Column(name = "total_accounts")
    private Integer totalAccounts;

    @Column(name = "active_accounts")
    private Integer activeAccounts;

    @Column(name = "closed_accounts")
    private Integer closedAccounts;

    @Column(name = "default_accounts")
    private Integer defaultAccounts;

    @Column(name = "outstanding_balance")
    private BigDecimal outstandingBalance;

    @Column(name = "raw_response", columnDefinition = "JSON")
    private String rawResponse;

    @Column(name = "no_record_found")
    private Boolean noRecordFound;

    @Column(name = "report_data", columnDefinition = "JSON")
    private String reportData;

    @Column(nullable = false, length = 30)
    private String status = "pending";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (checkedAt == null) checkedAt = LocalDateTime.now();
    }
}
