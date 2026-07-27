package com.sabbpe.repository;

import com.sabbpe.model.MerchantPersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantPersonRepository extends JpaRepository<MerchantPersonEntity, String> {

    List<MerchantPersonEntity> findByMerchantIdOrderBySequenceOrderAsc(String merchantId);
}
