package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.DocumentValidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentValidationRepository extends JpaRepository<DocumentValidationEntity, String> {
    List<DocumentValidationEntity> findByDocumentId(String documentId);
    List<DocumentValidationEntity> findByMerchantId(String merchantId);
}
