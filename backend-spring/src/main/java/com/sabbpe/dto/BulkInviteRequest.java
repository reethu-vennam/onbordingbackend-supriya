package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkInviteRequest {
    private List<MerchantInvite> merchants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantInvite {
        private String fullName;
        private String mobileNumber;
        private String email;
        private String businessName;
    }
}
