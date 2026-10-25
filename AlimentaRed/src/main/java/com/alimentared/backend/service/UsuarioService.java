package com.alimentared.backend.service;

import com.alimentared.backend.dto.request.CambioContrasenaRequest;
import com.alimentared.backend.dto.request.UsuarioInternoRequest;
import com.alimentared.backend.dto.response.UsuarioResponse;

public interface UsuarioService {

    UsuarioResponse obtenerDatosPerfil(Long usuarioId);

    UsuarioResponse registrarUsuarioInterno(UsuarioInternoRequest request);

    void cambiarContrasena(Long usuarioId, CambioContrasenaRequest request);
}
