package com.sabbpe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest {

    @NotBlank
    @JsonProperty("full_name")
    private String fullName;

    @NotBlank
    @JsonProperty("mobile_number")
    private String mobileNumber;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
