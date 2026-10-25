package com.alimentared.backend.controller;

import com.alimentared.backend.config.UsuarioPrincipal;
import com.alimentared.backend.dto.request.CambioContrasenaRequest;
import com.alimentared.backend.dto.request.UsuarioInternoRequest;
import com.alimentared.backend.dto.response.UsuarioResponse;
import com.alimentared.backend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Usuarios", description = "Perfil del usuario autenticado y usuarios internos")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Datos del usuario en sesión")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.obtenerDatosPerfil(principal.getId()));
    }

    @Operation(summary = "Cambiar contraseña del usuario en sesión")
    @PatchMapping("/me/contrasena")
    public ResponseEntity<Void> cambiarContrasena(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                  @Valid @RequestBody CambioContrasenaRequest request) {
        usuarioService.cambiarContrasena(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear coordinador o administrador")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuarioInterno(@Valid @RequestBody UsuarioInternoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarUsuarioInterno(request));
    }
}
