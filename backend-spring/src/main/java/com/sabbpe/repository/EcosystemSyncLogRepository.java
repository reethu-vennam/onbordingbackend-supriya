package com.sabbpe.repository;

import com.sabbpe.model.EcosystemSyncLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EcosystemSyncLogRepository extends JpaRepository<EcosystemSyncLogEntity, String> {

    List<EcosystemSyncLogEntity> findByStatusAndAttemptCountLessThan(String status, Integer maxAttempts);

    List<EcosystemSyncLogEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
}
