package com.sabbpe.service;

import com.sabbpe.dto.BankValidationResponse;
import com.sabbpe.dto.OcrResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private static final int OCR_MAX_IMAGE_BYTES = 80 * 1024;

    public OcrResult documentOcr(MultipartFile file, String docType) throws Exception {
        String normalizedDocType = normalizeDocType(docType);

        String token = generateToken();
        if (token == null) {
            throw new IllegalStateException("Failed to generate Transbank token");
        }

        byte[] uploadedBytes = file.getBytes();
        byte[] jpegBytes = isJpeg(uploadedBytes) ? uploadedBytes : writeJpeg(readImage(uploadedBytes), 0.95f);
        if (jpegBytes.length > OCR_MAX_IMAGE_BYTES) {
            jpegBytes = compressJpegToLimit(readImage(jpegBytes), OCR_MAX_IMAGE_BYTES);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("doc_front_image", Base64.getEncoder().encodeToString(jpegBytes));
        body.put("doc_type", normalizedDocType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/ocr", entity, JsonNode.class);

        log.info("Transbank OCR - status: {}, jpegSizeBytes: {}", response.getStatusCode(), jpegBytes.length);

        return parseOcrResult(response.getBody(), normalizedDocType);
    }

    private OcrResult parseOcrResult(JsonNode raw, String docType) {
        JsonNode result = raw;
        if (result != null && result.has("result")) {
            result = result.get("result");
        }
        if (result != null && result.has("data")) {
            result = result.get("data");
        }

        String panNumber = null;
        String aadhaarNumber = null;
        String name = null;
        String dob = null;

        if ("PAN".equals(docType)) {
            panNumber = firstText(result, "card_number", "pan_number", "pan", "panNumber");
            name = firstText(result, "name_on_card", "name", "nameOnCard");
            dob = firstText(result, "date_of_birth", "dob", "dateOfBirth");
        } else {
            aadhaarNumber = firstText(result, "aadhaar_number", "aadhaarNumber", "aadhaar", "uid", "card_number");
            name = firstText(result, "name_on_card", "name", "nameOnCard");
            dob = firstText(result, "date_of_birth", "dob", "dateOfBirth");
        }

        return OcrResult.builder()
                .panNumber(panNumber)
                .aadhaarNumber(aadhaarNumber)
                .extractedName(name)
                .dateOfBirth(dob)
                .confidence(90)
                .rawText(raw != null ? raw.toString() : null)
                .build();
    }

    private static String firstText(JsonNode node, String... fields) {
        if (node == null) return null;
        for (String field : fields) {
            if (node.has(field) && !node.get(field).isNull()) {
                return node.get(field).asText();
            }
        }
        return null;
    }

    private String normalizeDocType(String docType) {
        return docType == null ? "" : docType.trim().replace("\"", "").toUpperCase();
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes != null
                && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private BufferedImage readImage(byte[] bytes) throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) {
            throw new IllegalArgumentException("Invalid image file");
        }
        return toRgbImage(source);
    }

    private BufferedImage toRgbImage(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    private byte[] compressJpegToLimit(BufferedImage image, int maxBytes) throws Exception {
        BufferedImage current = image;
        byte[] best = null;

        for (int resizeAttempt = 0; resizeAttempt < 12; resizeAttempt++) {
            for (float quality = 0.90f; quality >= 0.35f; quality -= 0.05f) {
                byte[] candidate = writeJpeg(current, quality);
                if (best == null || candidate.length < best.length) {
                    best = candidate;
                }
                if (candidate.length <= maxBytes) {
                    return candidate;
                }
            }

            int nextWidth = Math.max(300, Math.round(current.getWidth() * 0.85f));
            int nextHeight = Math.max(300, Math.round(current.getHeight() * 0.85f));
            if (nextWidth == current.getWidth() && nextHeight == current.getHeight()) {
                break;
            }
            current = resizeImage(current, nextWidth, nextHeight);
        }

        if (best != null && best.length <= maxBytes) {
            return best;
        }
        throw new IllegalArgumentException("Unable to compress image below 80KB");
    }

    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("JPEG writer is not available");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(Math.max(0.0f, Math.min(1.0f, quality)));
            }
            writer.write(null, new IIOImage(image, null, null), params);
            imageOutput.flush();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

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
