package com.sabbpe.repository;

import com.sabbpe.model.MerchantSubProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantSubProductRepository extends JpaRepository<MerchantSubProductEntity, String> {

    List<MerchantSubProductEntity> findByMerchantProfileId(String merchantProfileId);

    List<MerchantSubProductEntity> findByMerchantProfileIdAndParentProductCode(String merchantProfileId, String parentProductCode);

    void deleteByMerchantProfileId(String merchantProfileId);
}
