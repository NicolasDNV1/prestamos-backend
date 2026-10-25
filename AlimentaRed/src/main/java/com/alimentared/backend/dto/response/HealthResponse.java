package com.alimentared.backend.dto.response;

import java.time.LocalDateTime;

public record HealthResponse(String estado, String servicio, LocalDateTime timestamp) {
}
