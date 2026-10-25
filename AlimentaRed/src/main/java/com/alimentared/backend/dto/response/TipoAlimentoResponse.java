package com.alimentared.backend.dto.response;

public record TipoAlimentoResponse(
        Long id,
        String nombre,
        String descripcion,
        String unidadMedida
) {
}
