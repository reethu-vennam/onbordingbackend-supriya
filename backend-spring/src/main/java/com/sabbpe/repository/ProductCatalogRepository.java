package com.sabbpe.repository;

import com.sabbpe.model.ProductCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCatalogRepository extends JpaRepository<ProductCatalogEntity, String> {

    Optional<ProductCatalogEntity> findByProductCode(String productCode);

    List<ProductCatalogEntity> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<ProductCatalogEntity> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(String category);
}
