package com.alimentared.backend.dto.request;

import com.alimentared.backend.entity.enums.Rol;
import jakarta.validation.constraints.*;

/** Alta de usuarios internos (Coordinador / Administrador) por parte del Administrador. */
public record UsuarioInternoRequest(
        @NotBlank @Size(max = 120)
        String nombre,

        @NotBlank @Email @Size(max = 150)
        String correo,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe contener letras y números")
        String contrasena,

        @NotNull
        Rol rol
) {
}
