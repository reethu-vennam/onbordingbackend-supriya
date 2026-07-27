package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductsRequest {

    private String selectedProducts;
    private List<SubProductSelection> subProducts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubProductSelection {
        private String parentProductCode;
        private List<String> subProductCodes;
    }
}
