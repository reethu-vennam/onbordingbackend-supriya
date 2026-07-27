package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.MerchantProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfileEntity, String> {
    Optional<MerchantProfileEntity> findByUserId(String userId);
    Optional<MerchantProfileEntity> findByBankApplicationId(String bankApplicationId);
    java.util.List<MerchantProfileEntity> findByOnboardingStatus(String status);
}
