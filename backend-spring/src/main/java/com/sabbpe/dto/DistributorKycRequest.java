package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributorKycRequest {

    private String panNumber;
    private String aadhaarLast4;
    private String panDocumentPath;
    private String aadhaarDocumentPath;
    private String profilePhotoPath;

    // KYC submit validation fields (from old Node.js)
    private String bankAccountNumber;
    private String bankIfsc;
    private String address;
}
