package com.sabbpe.repository;

import com.sabbpe.model.SettlementHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementHistoryRepository extends JpaRepository<SettlementHistoryEntity, String> {

    List<SettlementHistoryEntity> findByMerchantIdOrderBySettlementDateDesc(String merchantId);

    Page<SettlementHistoryEntity> findByMerchantIdOrderBySettlementDateDesc(String merchantId, Pageable pageable);

    List<SettlementHistoryEntity> findByStatus(String status);

    List<SettlementHistoryEntity> findByMerchantIdAndStatus(String merchantId, String status);
}
