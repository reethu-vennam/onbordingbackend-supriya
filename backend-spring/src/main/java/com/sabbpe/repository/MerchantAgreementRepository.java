package com.sabbpe.repository;

import com.sabbpe.model.MerchantAgreementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantAgreementRepository extends JpaRepository<MerchantAgreementEntity, String> {

    List<MerchantAgreementEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
}
