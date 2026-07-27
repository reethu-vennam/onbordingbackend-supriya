package com.sabbpe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMerchantRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String mobileNumber;

    @NotBlank @Email
    private String email;

    private String businessName;
    private String entityType;
    private String panNumber;
    private String gstNumber;

    @NotBlank @Size(min = 6)
    private String password;

    private BigDecimal commission;

    private Boolean rollingReserveEnabled;
    private BigDecimal rollingReservePercentage;
    private BigDecimal rollingReserveFixedInr;
    private Integer settlementCycleDays;
}
