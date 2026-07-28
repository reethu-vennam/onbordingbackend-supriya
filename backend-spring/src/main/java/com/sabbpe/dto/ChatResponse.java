package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String reply;
    private String currentStep;
    private String currentQuestion;
    private int questionIndex;
    private int totalQuestions;
    private boolean stepComplete;
    private Map<String, Object> collectedData;
}
