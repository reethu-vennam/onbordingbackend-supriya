package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendAgreementRequest {
    private String distributorId;
    private String agreementFilePath;
    private String fileBase64;
    private String fileName;
}
