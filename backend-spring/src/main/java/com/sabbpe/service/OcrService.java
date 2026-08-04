package com.sabbpe.service;

import com.sabbpe.dto.OcrResult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OcrService {

    @Value("${app.ocr.space-api-key:K88739396488957}")
    private String ocrSpaceApiKey;

    private static final String OCR_SPACE_URL = "https://api.ocr.space/parse/image";

    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}");
    private static final Pattern AADHAAR_PATTERN_NO_SEP = Pattern.compile("\\d{12}");

    private static final Set<String> EXCLUDE_WORDS = Set.of(
            "INCOME", "TAX", "DEPARTMENT", "GOVT", "GOVERNMENT", "INDIA",
            "PERMANENT", "ACCOUNT", "NUMBER", "SIGNATURE", "PAN", "UNIQUE", "IDENTIFICATION",
            "AUTHORITY", "AADHAAR", "MALE", "FEMALE", "DOB", "VID");

    private final RestTemplate restTemplate;

    public OcrService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public OcrResult extractDocument(MultipartFile file) {
        try {
            String rawText = callOcrSpace(file);
            if (rawText == null || rawText.isBlank()) {
                return OcrResult.builder().confidence(0).build();
            }
            return parseDocumentText(rawText);
        } catch (Exception e) {
            log.error("OCR extraction failed", e);
            return OcrResult.builder().confidence(0).rawText(e.getMessage()).build();
        }
    }

    private String callOcrSpace(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("apikey", ocrSpaceApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("language", "eng");
            body.add("isOverlayRequired", "false");
            body.add("OCREngine", "2");

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.jpg";
                }
            };
            body.add("file", resource);

            log.info("Calling OCR.space for file: {} ({}KB)", file.getOriginalFilename(), file.getSize() / 1024);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(OCR_SPACE_URL, entity, Map.class);

            log.info("OCR.space response status: {}", response.getStatusCode());

            if (response.getBody() != null) {
                log.info("OCR.space response body keys: {}", response.getBody().keySet());
                Object isErrored = response.getBody().get("IsErroredOnProcessing");
                if (isErrored instanceof Boolean && (Boolean) isErrored) {
                    log.error("OCR.space error: {}", response.getBody().get("ErrorMessage"));
                    return null;
                }
                Object results = response.getBody().get("ParsedResults");
                if (results instanceof List && !((List) results).isEmpty()) {
                    Map firstResult = (Map) ((List) results).get(0);
                    String parsedText = (String) firstResult.get("ParsedText");
                    log.info("OCR.space parsed text: {}",
                            parsedText != null ? parsedText.substring(0, Math.min(parsedText.length(), 200)) : "null");
                    return parsedText;
                } else {
                    log.warn("OCR.space no parsed results. Full response: {}", response.getBody());
                }
            }
            return null;
        } catch (Exception e) {
            log.error("OCR.space API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private OcrResult parseDocumentText(String rawText) {
        String clean = rawText.toUpperCase().replaceAll("[^A-Z0-9\\s]", " ").replaceAll("\\s+", " ");
        String cleanForRegex = rawText.replaceAll("[^\\w\\s/-]", " ").replaceAll("\\s+", " ");

        List<String> lines = Arrays.stream(rawText.split("\n"))
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .collect(Collectors.toList());

        boolean isPan = detectPan(clean);
        boolean isAadhaar = detectAadhaar(clean);

        String panNumber = extractPanNumber(cleanForRegex);
        String aadhaarNumber = extractAadhaarNumber(cleanForRegex);
        String extractedName = extractName(lines);
        String dateOfBirth = extractDob(cleanForRegex);

        if (isPan && panNumber != null) {
            return OcrResult.builder()
                    .panNumber(panNumber)
                    .extractedName(extractedName)
                    .dateOfBirth(dateOfBirth)
                    .confidence(90)
                    .rawText(rawText)
                    .build();
        }
        if (isAadhaar && aadhaarNumber != null) {
            return OcrResult.builder()
                    .aadhaarNumber(aadhaarNumber)
                    .extractedName(extractedName)
                    .dateOfBirth(dateOfBirth)
                    .confidence(90)
                    .rawText(rawText)
                    .build();
        }
        if (panNumber != null) {
            return OcrResult.builder()
                    .panNumber(panNumber)
                    .extractedName(extractedName)
                    .dateOfBirth(dateOfBirth)
                    .confidence(70)
                    .rawText(rawText)
                    .build();
        }
        if (aadhaarNumber != null) {
            return OcrResult.builder()
                    .aadhaarNumber(aadhaarNumber)
                    .extractedName(extractedName)
                    .dateOfBirth(dateOfBirth)
                    .confidence(70)
                    .rawText(rawText)
                    .build();
        }
        return OcrResult.builder().confidence(0).rawText(rawText).build();
    }

    private boolean detectPan(String clean) {
        List<Pattern> panPatterns = List.of(
                Pattern.compile("INCOME\\s*TAX"),
                Pattern.compile("PERMANENT\\s*ACCOUNT"),
                Pattern.compile("GOVT?\\s*OF\\s*INDIA"),
                Pattern.compile("\\bPAN\\b"));
        long score = panPatterns.stream().filter(p -> p.matcher(clean).find()).count();
        if (Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]").matcher(clean).find())
            score += 2;
        return score > 0;
    }

    private boolean detectAadhaar(String clean) {
        List<Pattern> aadhaarPatterns = List.of(
                Pattern.compile("UNIQUE\\s*IDENTIFICATION"),
                Pattern.compile("GOVERNMENT\\s*OF\\s*INDIA"),
                Pattern.compile("AADHAAR"),
                Pattern.compile("\\bUID\\b"));
        long score = aadhaarPatterns.stream().filter(p -> p.matcher(clean).find()).count();
        if (AADHAAR_PATTERN.matcher(clean).find())
            score += 2;
        return score > 0;
    }

    private String extractPanNumber(String text) {
        Matcher m = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]").matcher(text);
        return m.find() ? m.group() : null;
    }

    private String extractAadhaarNumber(String text) {
        Matcher m = AADHAAR_PATTERN.matcher(text);
        if (m.find()) {
            String raw = m.group().replaceAll("[\\s-]", "");
            return raw.replaceAll("(\\d{4})(\\d{4})(\\d{4})", "$1 $2 $3");
        }
        m = AADHAAR_PATTERN_NO_SEP.matcher(text);
        if (m.find()) {
            String raw = m.group();
            return raw.replaceAll("(\\d{4})(\\d{4})(\\d{4})", "$1 $2 $3");
        }
        return null;
    }

    private String extractName(List<String> lines) {
        String best = "";
        int maxScore = 0;
        for (String line : lines) {
            String upper = line.toUpperCase();
            List<String> words = Arrays.asList(upper.split("\\s+"));
            boolean hasExcluded = words.stream().anyMatch(w -> EXCLUDE_WORDS.stream().anyMatch(w::contains));
            boolean isAllCaps = upper.equals(line.trim());
            boolean hasLetters = Pattern.compile("[A-Z]").matcher(line).find();
            boolean hasNumbers = Pattern.compile("[0-9]").matcher(line).find();
            if (!hasExcluded && isAllCaps && hasLetters && !hasNumbers && line.trim().length() > 3) {
                int score = line.length() + words.size() * 2;
                if (score > maxScore) {
                    maxScore = score;
                    best = line.trim();
                }
            }
        }
        return best.isEmpty() ? null : best;
    }

    private String extractDob(String text) {
        Matcher m = Pattern.compile("(\\d{1,2})[\\/\\-.]\\s*(\\d{1,2})[\\/\\-.]\\s*(\\d{4})").matcher(text);
        return m.find() ? m.group() : null;
    }
}
