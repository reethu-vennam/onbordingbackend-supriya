package com.sabbpe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_sub_products", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"merchant_profile_id", "sub_product_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSubProductEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_profile_id", nullable = false, length = 36)
    private String merchantProfileId;

    @Column(name = "parent_product_code", nullable = false, length = 50)
    private String parentProductCode;

    @Column(name = "sub_product_code", nullable = false, length = 50)
    private String subProductCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }
}
