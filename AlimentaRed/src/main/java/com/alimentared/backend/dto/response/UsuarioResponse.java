package com.alimentared.backend.dto.response;

import com.alimentared.backend.entity.enums.Rol;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        Rol rol,
        boolean activo,
        LocalDateTime fechaRegistro,
        OrganizacionResponse organizacion
) {
}
