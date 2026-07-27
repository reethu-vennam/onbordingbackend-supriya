package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.MerchantDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantDocumentRepository extends JpaRepository<MerchantDocumentEntity, String> {
    List<MerchantDocumentEntity> findByMerchantId(String merchantId);
}
