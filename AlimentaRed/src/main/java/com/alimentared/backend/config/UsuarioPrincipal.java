package com.alimentared.backend.config;

import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.Rol;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String correo;
    private final String contrasena;
    private final Rol rol;
    private final Long organizacionId;
    private final boolean activo;
    private final boolean bloqueado;

    private UsuarioPrincipal(Long id, String correo, String contrasena, Rol rol,
                             Long organizacionId, boolean activo, boolean bloqueado) {
        this.id = id;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rol = rol;
        this.organizacionId = organizacionId;
        this.activo = activo;
        this.bloqueado = bloqueado;
    }

    public static UsuarioPrincipal from(Usuario usuario) {
        Organizacion organizacion = usuario.getOrganizacion();
        return new UsuarioPrincipal(
                usuario.getIdUsuario(),
                usuario.getCorreo(),
                usuario.getContrasena(),
                usuario.getRol(),
                organizacion != null ? organizacion.getIdOrganizacion() : null,
                usuario.isActivo(),
                organizacion != null && organizacion.isBloqueada());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return contrasena;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
