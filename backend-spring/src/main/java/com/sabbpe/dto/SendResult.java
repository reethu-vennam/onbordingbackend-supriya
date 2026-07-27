package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendResult {
    private boolean success;
    private String error;
    private Object status;
}
