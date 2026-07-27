package com.sabbpe.repository;

import com.sabbpe.model.MerchantKycEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantKycRepository extends JpaRepository<MerchantKycEntity, String> {

    Optional<MerchantKycEntity> findByMerchantId(String merchantId);

    void deleteByMerchantId(String merchantId);
}
