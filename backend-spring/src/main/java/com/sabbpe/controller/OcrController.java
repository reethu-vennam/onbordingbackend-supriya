package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.dto.OcrResult;
import com.sabbpe.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<OcrResult>> extractDocument(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BAD_REQUEST", "No file provided"));
        }
        log.info("OCR request for file: {}", file.getOriginalFilename());
        OcrResult result = ocrService.extractDocument(file);
        if (result.getConfidence() > 0) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        return ResponseEntity.ok(ApiResponse.error("OCR_FAILED", "Could not extract document data", result));
    }
}
