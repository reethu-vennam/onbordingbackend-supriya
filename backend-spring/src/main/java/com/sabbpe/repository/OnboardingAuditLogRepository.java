package com.sabbpe.repository;

import com.sabbpe.model.OnboardingAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OnboardingAuditLogRepository extends JpaRepository<OnboardingAuditLogEntity, String> {

    List<OnboardingAuditLogEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
}
