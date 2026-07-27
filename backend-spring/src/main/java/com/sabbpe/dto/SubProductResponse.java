package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubProductResponse {

    private String id;
    private String parentProductCode;
    private String productCode;
    private String productName;
    private String productDescription;
    private BigDecimal price;
    private boolean isActive;
    private Integer displayOrder;
}
