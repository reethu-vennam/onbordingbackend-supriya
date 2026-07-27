package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.MerchantKycEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantKycRepository extends JpaRepository<MerchantKycEntity, String> {
    Optional<MerchantKycEntity> findByMerchantId(String merchantId);
}
