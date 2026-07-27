package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.SupportKycActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportKycActionRepository extends JpaRepository<SupportKycActionEntity, String> {
}
