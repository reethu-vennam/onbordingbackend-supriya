package com.sabbpe.repository;

import com.sabbpe.model.MerchantProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfileEntity, String> {

    Optional<MerchantProfileEntity> findByUserId(String userId);

    Optional<MerchantProfileEntity> findByEcosystemOrganizationId(String ecosystemOrganizationId);

    Optional<MerchantProfileEntity> findByEmail(String email);

    Optional<MerchantProfileEntity> findByMobileNumber(String mobileNumber);

    Optional<MerchantProfileEntity> findByBusinessName(String businessName);

    List<MerchantProfileEntity> findByDistributorId(String distributorId);

    List<MerchantProfileEntity> findByOnboardingStatus(String status);

    List<MerchantProfileEntity> findByDistributorIdAndOnboardingStatus(String distributorId, String status);

    Optional<MerchantProfileEntity> findByTransactionId(String transactionId);

    Optional<MerchantProfileEntity> findByBankApplicationId(String bankApplicationId);

    @Query("SELECT m FROM MerchantProfileEntity m WHERE m.settlementTermsLocked = false AND m.distributorId IS NOT NULL AND m.email IS NOT NULL")
    List<MerchantProfileEntity> findEligibleForSettlement();

    long countByDistributorId(String distributorId);

    long countByDistributorIdAndOnboardingStatus(String distributorId, String status);
}
