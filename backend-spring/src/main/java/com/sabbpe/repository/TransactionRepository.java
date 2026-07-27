package com.sabbpe.repository;

import com.sabbpe.model.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    List<TransactionEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId);

    List<TransactionEntity> findByMerchantIdIn(List<String> merchantIds);

    boolean existsByTransactionId(String transactionId);
}
