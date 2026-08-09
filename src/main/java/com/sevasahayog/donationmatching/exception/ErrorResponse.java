package com.sevasahayog.donationmatching.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields
) {

    public static ErrorResponse of(HttpStatus status, String error, String message, String path) {
        return of(status, error, message, path, null);
    }

    public static ErrorResponse of(HttpStatus status, String error, String message, String path,
                                   Map<String, String> fields) {
        return new ErrorResponse(Instant.now().toString(), status.value(), error, message, path, fields);
    }
}
