package com.sabbpe.repository;

import com.sabbpe.model.MerchantDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantDocumentRepository extends JpaRepository<MerchantDocumentEntity, String> {

    List<MerchantDocumentEntity> findByMerchantId(String merchantId);

    List<MerchantDocumentEntity> findByMerchantIdAndDocumentType(String merchantId, String documentType);

    long countByMerchantId(String merchantId);
}
