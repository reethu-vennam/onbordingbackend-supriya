package com.sabbpe.repository;

import com.sabbpe.model.DistributorRecoveryHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DistributorRecoveryHistoryRepository extends JpaRepository<DistributorRecoveryHistoryEntity, String> {

    List<DistributorRecoveryHistoryEntity> findByDistributorIdOrderByCreatedAtDesc(String distributorId);

    List<DistributorRecoveryHistoryEntity> findByChargebackId(String chargebackId);
}
