package com.sabbpe.repository;

import com.sabbpe.model.MerchantBankDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantBankDetailRepository extends JpaRepository<MerchantBankDetailEntity, String> {

    List<MerchantBankDetailEntity> findByMerchantId(String merchantId);

    void deleteByMerchantId(String merchantId);
}
