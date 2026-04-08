package com.example.riskmonitoring.producer.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * HTTP status code.
     */
    private int status;

    /**
     * Error message.
     */
    private String message;

    /**
     * Detailed error information.
     */
    private String detail;

    /**
     * Validation errors mapped by field name.
     */
    private Map<String, String> errors;
}
