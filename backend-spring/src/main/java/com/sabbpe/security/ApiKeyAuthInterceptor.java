package com.sabbpe.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    @Value("${app.files.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "File API key is not configured");
            return false;
        }
        String provided = request.getHeader("X-API-Key");
        if (provided == null || !provided.equals(apiKey)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Invalid or missing X-API-Key");
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}
