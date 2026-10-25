package com.alimentared.backend.dto.response;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String codigo,
        String mensaje,
        String path,
        List<String> detalles
) {

    public static ErrorResponse of(HttpStatus status, String codigo, String mensaje, String path, List<String> detalles) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                codigo, mensaje, path, detalles);
    }
}
