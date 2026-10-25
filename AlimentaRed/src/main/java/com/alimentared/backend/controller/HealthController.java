package com.alimentared.backend.controller;

import com.alimentared.backend.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

@Tag(name = "Sistema", description = "Estado del servidor")
@SecurityRequirements
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final Clock clock;

    @Operation(summary = "Verificar disponibilidad del servidor")
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", "AlimentaRed API", LocalDateTime.now(clock)));
    }
}
