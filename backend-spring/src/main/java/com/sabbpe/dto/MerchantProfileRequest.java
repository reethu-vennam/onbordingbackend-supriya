package com.sabbpe.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantProfileRequest {

    private String fullName;
    private String mobileNumber;
    @Email
    private String email;
    private String panNumber;
    private String aadhaarNumber;
    private String businessName;
    private String gstNumber;
    private String entityType;

    private String businessAddressLine1;
    private String businessAddressLine2;
    private String businessCity;
    private String businessState;
    private String businessPostalCode;
    private String businessCountry;

    @JsonDeserialize(using = RawJsonDeserializer.class)
    private String selectedProducts;

    @JsonDeserialize(using = RawJsonDeserializer.class)
    private String scanResults;

    private List<PersonRequest> persons;
    private List<BankDetailRequest> bankDetails;
    private KycRequest kyc;
    private List<DocumentRequest> documents;

    public static class RawJsonDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isTextual()) {
                return node.asText();
            }
            return node.toString();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycRequest {
        private Boolean videoKycCompleted;
        private Boolean locationCaptured;
        private String selfieUrl;
        private Boolean isVideoCompleted;
        private Boolean locationVerified;
        private Double latitude;
        private Double longitude;
        private String fullAddress;
        private String area;
        private String city;
        private String state;
        private String pincode;
        private String country;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentRequest {
        private String fileName;
        private String filePath;
        private String documentType;
        private String docCategory;
        private Long fileSize;
        private String mimeType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonRequest {
        private String role;
        private String fullName;
        private String panNumber;
        private String addressProofType;
        private Boolean isAuthorizedSignatory;
        private Integer sequenceOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankDetailRequest {
        private String accountNumber;
        private String ifscCode;
        private String bankName;
        private String accountHolderName;
        private String upiVpa;
    }
}
