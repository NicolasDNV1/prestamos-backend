package com.alimentared.backend.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OrganizacionResponse(
        Long id,
        String nit,
        String nombre,
        String ubicacion,
        String correo,
        String telefono,
        boolean verificada,
        boolean bloqueada,
        int strikesVigentes,
        LocalDateTime fechaRegistro
) {
}
