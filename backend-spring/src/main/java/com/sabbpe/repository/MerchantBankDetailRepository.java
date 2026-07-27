package com.sabbpe.repository;

import com.sabbpe.model.MerchantBankDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantBankDetailRepository extends JpaRepository<MerchantBankDetailEntity, String> {

    Optional<MerchantBankDetailEntity> findByMerchantId(String merchantId);

    void deleteByMerchantId(String merchantId);
}
