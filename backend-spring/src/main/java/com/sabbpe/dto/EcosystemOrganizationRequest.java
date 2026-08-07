package com.sabbpe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EcosystemOrganizationRequest {

    @JsonProperty("organization_id")
    private String organizationId;

    @JsonProperty("parent_organization_id")
    private String parentOrganizationId;

    @JsonProperty("organization_code")
    private String organizationCode;

    @JsonProperty("legal_name")
    private String legalName;

    @JsonProperty("trade_name")
    private String tradeName;

    private String email;
    private String mobile;
    private String website;

    @JsonProperty("domain_name")
    private String domainName;

    @JsonProperty("gst_number")
    private String gstNumber;

    @JsonProperty("pan_number")
    private String panNumber;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("timezone_name")
    private String timezoneName;

    private String status;
    private Map<String, Object> metadata;

    @JsonProperty("version_no")
    private String versionNo;

    @JsonProperty("organization_type")
    private String organizationType;

    @JsonProperty("bank_accounts")
    private Object bankAccounts;

    @JsonProperty("callback_urls")
    private Object callbackUrls;

    private Object contacts;
    private Object documents;
    private Object products;
    private Object providers;
    private Object settings;

    @JsonProperty("transaction_aes_key")
    private String transactionAesKey;

    @JsonProperty("transaction_iv")
    private String transactionIv;

    @JsonProperty("transaction_key_version")
    private String transactionKeyVersion;

    @JsonProperty("transaction_key_algorithm")
    private String transactionKeyAlgorithm;

    @JsonProperty("transaction_key_created_on")
    private String transactionKeyCreatedOn;

    @JsonProperty("transaction_key_rotated_on")
    private String transactionKeyRotatedOn;

    @JsonProperty("transaction_key_status")
    private String transactionKeyStatus;

    @JsonProperty("transaction_userid")
    private String transactionUserid;

    @JsonProperty("transaction_merchantid")
    private String transactionMerchantid;

    @JsonProperty("transaction_password")
    private String transactionPassword;

    @JsonProperty("created_on")
    private String createdOn;

    @JsonProperty("modified_on")
    private String modifiedOn;

    @JsonProperty("secret_key")
    private String secretKey;
}
