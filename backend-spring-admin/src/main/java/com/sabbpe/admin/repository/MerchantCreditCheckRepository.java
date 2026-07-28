package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.MerchantCreditCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantCreditCheckRepository extends JpaRepository<MerchantCreditCheckEntity, String> {
    Optional<MerchantCreditCheckEntity> findByMerchantIdAndPanNumberAndExpiresAtAfter(
            String merchantId, String panNumber, LocalDateTime expiresAt);
    Optional<MerchantCreditCheckEntity> findTopByMerchantIdOrderByCheckedAtDesc(String merchantId);
    List<MerchantCreditCheckEntity> findByMerchantId(String merchantId);
}
