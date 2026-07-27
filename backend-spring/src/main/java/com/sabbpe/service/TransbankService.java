package com.sabbpe.service;

import com.sabbpe.dto.BankValidationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransbankService {

    @Value("${app.transbank.base-url:https://transbank.sabbpe.com/api}")
    private String baseUrl;

    @Value("${app.transbank.client-id:5e06f31d-d298-11f0-96ff-4201c0a81e02}")
    private String clientId;

    @Value("${app.transbank.entity-id:8116f9f2-f5d1-4e9c-9c2c-988015c140b4}")
    private String entityId;

    @Value("${app.transbank.program-id:513}")
    private String programId;

    @Value("${app.transbank.processor:TRANSBANK}")
    private String processor;

    @Value("${app.transbank.transaction-type:IMPS}")
    private String transactionType;

    private final RestTemplate restTemplate;

    private static final Set<String> SUCCESS_STATUSES = Set.of(
        "ACCOUNT_VALID", "VALID", "VALIDATED", "SUCCESS", "ACCOUNT_VERIFIED"
    );

    public String generateToken() {
        try {
            String url = baseUrl + "/v1/token/generate";
            String timestamp = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("client_Id", clientId);
            request.put("transaction_timestamp", timestamp);
            request.put("processor", processor);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);

            if (response.getBody() != null && response.getBody().has("token")) {
                log.info("Transbank token generated successfully");
                return response.getBody().get("token").asText();
            }
            log.error("Token not found in Transbank response: {}", response.getBody());
            return null;
        } catch (Exception e) {
            log.error("Error generating Transbank token", e);
            return null;
        }
    }

    public BankValidationResponse validateBankAccount(
            String custName, String custIfsc, String custAcctNo,
            String requestId, String trackingRefNo, String txnType) {
        return validateBankAccount(custName, custIfsc, custAcctNo, requestId, trackingRefNo, txnType, null);
    }

    public BankValidationResponse validateBankAccount(
            String custName, String custIfsc, String custAcctNo,
            String requestId, String trackingRefNo, String txnType,
            String token) {
        try {
            String authToken = token;
            if (authToken == null) {
                authToken = generateToken();
            }
            if (authToken == null) {
                return BankValidationResponse.builder()
                        .isValid(false)
                        .error("Failed to generate authorization token")
                        .build();
            }

            String reqId = requestId != null ? requestId : UUID.randomUUID().toString();
            String trackRef = trackingRefNo != null ? trackingRefNo : reqId.split("-")[0];

            String url = baseUrl + "/bank-account-validation";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("entityId", entityId);
            body.put("programId", programId);
            body.put("requestId", reqId);
            body.put("custName", custName);
            body.put("custIfsc", custIfsc);
            body.put("custAcctNo", custAcctNo);
            body.put("trackingRefNo", trackRef);
            body.put("txnType", txnType != null ? txnType : transactionType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);

            return parseValidationResponse(response.getBody(), reqId, trackRef);
        } catch (Exception e) {
            log.error("Error validating bank account via Transbank", e);
            return BankValidationResponse.builder()
                    .isValid(false)
                    .message("Account validation failed")
                    .error(e.getMessage())
                    .build();
        }
    }

    private BankValidationResponse parseValidationResponse(JsonNode raw, String fallbackRequestId, String fallbackTrackingRefNo) {
        if (raw == null) {
            return BankValidationResponse.builder().isValid(false).message("Empty response from Transbank").build();
        }

        JsonNode data = raw.has("data") ? raw.get("data") : raw;

        JsonNode resultPayload = data;
        if (data.has("result")) {
            JsonNode result = data.get("result");
            if (result.isArray() && result.size() > 0) {
                result = result.get(0);
            }
            resultPayload = result;
        }

        String statusCode = firstOf(raw, "statusCode", data, "statusCode", resultPayload, "statusCode");
        String status = firstOf(raw, "status", data, "status", resultPayload, "status");
        String message = firstOf(raw, "message", data, "message", resultPayload, "message");
        String acValidationStatus = firstOf(raw, "acValidationStatus", data, "acValidationStatus", resultPayload, "acValidationStatus");
        String validationStatus = firstOf(raw, "validationStatus", data, "validationStatus", resultPayload, "validationStatus");
        String responseId = firstOf(raw, "responseId", data, "responseId", resultPayload, "responseId");
        String responseRequestId = firstOf(raw, "requestId", data, "requestId", resultPayload, "requestId");
        String nameAtBank = firstOf(
            raw, "nameAtBank", raw, "accountName", raw, "custName", raw, "name",
            data, "nameAtBank", data, "accountName", data, "custName", data, "name",
            resultPayload, "nameAtBank", resultPayload, "accountName", resultPayload, "custName", resultPayload, "name",
            resultPayload, "acName"
        );

        String effectiveStatus = acValidationStatus != null ? acValidationStatus : validationStatus;
        boolean isValid = isTrue(raw, "isValid")
                       || isTrue(data, "isValid")
                       || isTrue(resultPayload, "isValid")
                       || (effectiveStatus != null && SUCCESS_STATUSES.contains(effectiveStatus.toUpperCase()));

        BankValidationResponse.BankValidationResponseBuilder builder = BankValidationResponse.builder()
                .isValid(isValid)
                .accountName(nameAtBank)
                .requestId(responseRequestId != null ? responseRequestId : fallbackRequestId)
                .trackingRefNo(fallbackTrackingRefNo)
                .responseId(responseId)
                .statusCode(statusCode)
                .status(status)
                .message(message)
                .rawResponse(raw);

        if (isValid) {
            builder.accountStatus("verified");
        } else {
            builder.accountStatus(null);
            builder.error(message != null ? message : "Account validation failed");
        }

        return builder.build();
    }

    private static String firstOf(Object... pairs) {
        for (int i = 0; i < pairs.length - 1; i += 2) {
            if (pairs[i] instanceof JsonNode node && pairs[i + 1] instanceof String field) {
                if (node.has(field) && !node.get(field).isNull()) {
                    return node.get(field).asText();
                }
            }
        }
        return null;
    }

    private static boolean isTrue(JsonNode node, String field) {
        return node != null && node.has(field) && node.get(field).isBoolean() && node.get(field).asBoolean();
    }
}
