package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.MerchantBankDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantBankDetailRepository extends JpaRepository<MerchantBankDetailEntity, String> {
    Optional<MerchantBankDetailEntity> findByMerchantId(String merchantId);
}
