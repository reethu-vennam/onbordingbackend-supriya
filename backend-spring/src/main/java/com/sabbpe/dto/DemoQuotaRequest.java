package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoQuotaRequest {
    private String checkType;
    private Map<String, Object> inputData;
    private Map<String, Object> resultSummary;
    private String status;
    private String errorMessage;
}
