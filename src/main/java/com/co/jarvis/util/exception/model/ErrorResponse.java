package com.co.jarvis.util.exception.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String message;
    private String status;
    private int code;
}
