package com.sabbpe.repository;

import com.sabbpe.model.ProductSubCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSubCatalogRepository extends JpaRepository<ProductSubCatalogEntity, String> {

    List<ProductSubCatalogEntity> findByParentProductCodeAndIsActiveTrueOrderByDisplayOrderAsc(String parentProductCode);

    List<ProductSubCatalogEntity> findByParentProductCode(String parentProductCode);
}
