package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @PostMapping("/file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filePath", required = false) String filePath) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "No file provided"));
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String storedPath;
            if (filePath != null && !filePath.isBlank()) {
                storedPath = filePath;
            } else {
                storedPath = UUID.randomUUID().toString() + extension;
            }

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path fileDir = uploadPath.resolve(storedPath).getParent();
            if (fileDir != null && !Files.exists(fileDir)) {
                Files.createDirectories(fileDir);
            }

            Path fileDestination = uploadPath.resolve(storedPath);
            Files.copy(file.getInputStream(), fileDestination);

            log.info("File uploaded: {} -> {}", originalFilename, fileDestination);

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "url", "/uploads/" + storedPath,
                    "filename", storedPath,
                    "originalName", originalFilename,
                    "size", file.getSize(),
                    "mimeType", file.getContentType()
            )));
        } catch (IOException e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("UPLOAD_ERROR", "File upload failed: " + e.getMessage()));
        }
    }
}
