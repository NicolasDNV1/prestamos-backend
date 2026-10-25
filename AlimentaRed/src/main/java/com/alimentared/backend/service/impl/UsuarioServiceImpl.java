package com.alimentared.backend.service.impl;

import com.alimentared.backend.dto.request.CambioContrasenaRequest;
import com.alimentared.backend.dto.request.UsuarioInternoRequest;
import com.alimentared.backend.dto.response.UsuarioResponse;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.Rol;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.exception.DuplicateResourceException;
import com.alimentared.backend.mapper.UsuarioMapper;
import com.alimentared.backend.repository.UsuarioRepository;
import com.alimentared.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final Set<Rol> ROLES_INTERNOS = Set.of(Rol.COORDINADOR, Rol.ADMINISTRADOR);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final AccesoHelper accesoHelper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerDatosPerfil(Long usuarioId) {
        return usuarioMapper.toResponse(accesoHelper.usuario(usuarioId));
    }

    @Override
    @Transactional
    public UsuarioResponse registrarUsuarioInterno(UsuarioInternoRequest request) {
        if (!ROLES_INTERNOS.contains(request.rol())) {
            throw new BusinessRuleException("Los donantes y receptoras se registran con su organización en /api/auth/register");
        }
        String correo = request.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new DuplicateResourceException("Ya existe un usuario registrado con el correo " + correo);
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre().trim())
                .correo(correo)
                .contrasena(passwordEncoder.encode(request.contrasena()))
                .rol(request.rol())
                .fechaRegistro(LocalDateTime.now(clock))
                .build());
        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional
    public void cambiarContrasena(Long usuarioId, CambioContrasenaRequest request) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        if (!passwordEncoder.matches(request.contrasenaActual(), usuario.getContrasena())) {
            throw new BusinessRuleException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.contrasenaNueva(), usuario.getContrasena())) {
            throw new BusinessRuleException("La nueva contraseña debe ser distinta a la actual");
        }
        usuario.setContrasena(passwordEncoder.encode(request.contrasenaNueva()));
    }
}
