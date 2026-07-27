package com.sabbpe.repository;

import com.sabbpe.model.ChargebackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargebackRepository extends JpaRepository<ChargebackEntity, String> {

    Page<ChargebackEntity> findByMerchantId(String merchantId, Pageable pageable);

    List<ChargebackEntity> findByMerchantIdOrderByChargebackDateDesc(String merchantId);

    List<ChargebackEntity> findByMerchantIdAndStatus(String merchantId, String status);

    List<ChargebackEntity> findByStatus(String status);
}
