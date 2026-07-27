package com.sabbpe.admin.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_bank_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantBankDetailEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false)
    private String ifscCode;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
