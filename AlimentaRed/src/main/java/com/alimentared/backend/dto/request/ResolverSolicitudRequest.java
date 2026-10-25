package com.alimentared.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolverSolicitudRequest(
        @NotNull(message = "Debe indicar si la solicitud se aprueba o se rechaza")
        Boolean aprobada,

        @Size(max = 500)
        String motivo
) {

    @JsonIgnore
    @AssertTrue(message = "El motivo es obligatorio al rechazar una solicitud")
    public boolean isMotivoValido() {
        return !Boolean.FALSE.equals(aprobada) || (motivo != null && !motivo.isBlank());
    }
}
