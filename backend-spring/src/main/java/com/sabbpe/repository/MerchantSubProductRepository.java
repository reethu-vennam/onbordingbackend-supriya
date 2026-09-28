package com.sabbpe.repository;

import com.sabbpe.model.MerchantSubProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MerchantSubProductRepository extends JpaRepository<MerchantSubProductEntity, String> {

    List<MerchantSubProductEntity> findByMerchantProfileId(String merchantProfileId);

    List<MerchantSubProductEntity> findByMerchantProfileIdAndParentProductCode(String merchantProfileId, String parentProductCode);

    void deleteByMerchantProfileId(String merchantProfileId);

    @Modifying
    @Query(value = "INSERT INTO merchant_sub_products (id, merchant_profile_id, parent_product_code, sub_product_code, created_at) "
            + "VALUES (:id, :merchantProfileId, :parentProductCode, :subProductCode, :createdAt) "
            + "ON DUPLICATE KEY UPDATE parent_product_code = VALUES(parent_product_code)",
            nativeQuery = true)
    void upsertSubProduct(@Param("id") String id,
                          @Param("merchantProfileId") String merchantProfileId,
                          @Param("parentProductCode") String parentProductCode,
                          @Param("subProductCode") String subProductCode,
                          @Param("createdAt") LocalDateTime createdAt);
}
