package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
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
                // Sanitize: remove leading slashes, use forward slashes
                storedPath = filePath.replace("\\", "/").replaceAll("^/+", "");
            } else {
                storedPath = UUID.randomUUID().toString() + extension;
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path fileDestination = uploadPath.resolve(storedPath).normalize();

            // Security: ensure resolved path is still inside uploadDir
            if (!fileDestination.startsWith(uploadPath)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "Invalid file path"));
            }

            // Create parent directories if needed
            Path parentDir = fileDestination.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Delete existing file if present, then copy
            Files.deleteIfExists(fileDestination);
            Files.copy(file.getInputStream(), fileDestination, StandardCopyOption.REPLACE_EXISTING);

            String publicUrl = "/uploads/" + storedPath;
            log.info("File uploaded: {} -> {}", originalFilename, fileDestination);

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "url", publicUrl,
                    "filename", storedPath,
                    "originalName", originalFilename != null ? originalFilename : "",
                    "size", file.getSize(),
                    "mimeType", file.getContentType() != null ? file.getContentType() : ""
            )));
        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("UPLOAD_ERROR", "File upload failed: " + e.getMessage()));
        }
    }
}
