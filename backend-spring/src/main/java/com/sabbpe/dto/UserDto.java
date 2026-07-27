package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String id;
    private String email;
    private String fullName;
    private String mobileNumber;
    private boolean isActive;
    private List<String> roles;
    private LocalDateTime createdAt;
}
