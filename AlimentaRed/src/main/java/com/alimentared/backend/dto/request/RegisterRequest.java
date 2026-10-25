package com.alimentared.backend.dto.request;

import com.alimentared.backend.entity.enums.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        @Size(max = 150)
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe contener letras y números")
        String contrasena,

        @NotNull(message = "El rol es obligatorio (DONANTE o RECEPTORA)")
        Rol rol,

        @NotNull(message = "Los datos de la organización son obligatorios")
        @Valid
        OrganizacionRequest organizacion
) {
}
