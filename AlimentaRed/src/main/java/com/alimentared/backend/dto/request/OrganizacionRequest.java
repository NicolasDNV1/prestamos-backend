package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrganizacionRequest(
        @NotBlank(message = "El NIT es obligatorio")
        @Pattern(regexp = "^[0-9]{6,15}(-[0-9])?$", message = "Formato de NIT inválido (ej. 900123456-7)")
        String nit,

        @NotBlank(message = "El nombre de la organización es obligatorio")
        @Size(max = 150)
        String nombre,

        @Size(max = 200)
        String ubicacion,

        @NotBlank(message = "El correo de la organización es obligatorio")
        @Email(message = "Formato de correo inválido")
        @Size(max = 150)
        String correo,

        @Pattern(regexp = "^[0-9+\\- ]{7,30}$", message = "Formato de teléfono inválido")
        String telefono
) {
}
