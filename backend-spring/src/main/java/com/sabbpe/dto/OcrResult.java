package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    private String panNumber;
    private String aadhaarNumber;
    private String extractedName;
    private String dateOfBirth;
    private int confidence;
    private String rawText;
}
