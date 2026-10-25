package com.alimentared.backend.mapper;

import com.alimentared.backend.dto.response.UsuarioResponse;
import com.alimentared.backend.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsuarioMapper {

    private final OrganizacionMapper organizacionMapper;

    public UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .activo(usuario.isActivo())
                .fechaRegistro(usuario.getFechaRegistro())
                .organizacion(organizacionMapper.toResponse(usuario.getOrganizacion()))
                .build();
    }
}
