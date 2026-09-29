package com.sabbpe.controller;

import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileDocumentController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @GetMapping("/document")
    public ResponseEntity<InputStreamResource> getDocument(@RequestParam("path") String path) {
        if (path == null || path.isBlank()) {
            throw new BadRequestException("path is required");
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();

        String raw = path.replace("\\", "/");
        if (raw.startsWith("/")) {
            raw = raw.substring(1);
        }
        if (raw.startsWith("uploads/")) {
            raw = raw.substring("uploads/".length());
        }

        if (raw.contains("..") || raw.contains("\0")) {
            throw new BadRequestException("Invalid file path");
        }

        Path resolved = root.resolve(raw).normalize();

        if (!resolved.startsWith(root)) {
            throw new BadRequestException("Invalid file path");
        }

        if (!Files.exists(resolved) || Files.isDirectory(resolved)) {
            throw new ResourceNotFoundException("File", "path", path);
        }

        try {
            Path canonicalRoot = root.toRealPath();
            Path canonicalResolved = resolved.toRealPath();
            if (!canonicalResolved.startsWith(canonicalRoot)) {
                throw new BadRequestException("Invalid file path");
            }
        } catch (IOException e) {
            throw new BadRequestException("Invalid file path");
        }

        try {
            long size = Files.size(resolved);
            String contentType = probeContentType(resolved);
            InputStream in = Files.newInputStream(resolved);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(size)
                    .body(new InputStreamResource(in));
        } catch (IOException e) {
            log.error("Failed to read file: {}", resolved, e);
            throw new ResourceNotFoundException("File", "path", path);
        }
    }

    private String probeContentType(Path path) {
        try {
            String type = Files.probeContentType(path);
            if (type != null) {
                return type;
            }
        } catch (IOException ignored) {
        }
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
