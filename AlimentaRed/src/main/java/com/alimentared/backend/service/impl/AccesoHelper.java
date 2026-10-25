package com.alimentared.backend.service.impl;

import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.Rol;
import com.alimentared.backend.exception.AccountBlockedException;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.exception.ForbiddenOperationException;
import com.alimentared.backend.exception.ResourceNotFoundException;
import com.alimentared.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Reglas de pertenencia y responsabilidad compartidas por los servicios. */
@Component
@RequiredArgsConstructor
public class AccesoHelper {

    private final UsuarioRepository usuarioRepository;

    public Usuario usuario(Long usuarioId) {
        return usuarioRepository.findWithOrganizacionByIdUsuario(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
    }

    public boolean esCoordinacion(Usuario usuario) {
        return usuario.getRol() == Rol.COORDINADOR || usuario.getRol() == Rol.ADMINISTRADOR;
    }

    public Organizacion organizacionDe(Usuario usuario) {
        if (usuario.getOrganizacion() == null) {
            throw new ForbiddenOperationException("El usuario no pertenece a ninguna organización");
        }
        return usuario.getOrganizacion();
    }

    public boolean perteneceA(Usuario usuario, Organizacion organizacion) {
        return usuario.getOrganizacion() != null && organizacion != null
                && Objects.equals(usuario.getOrganizacion().getIdOrganizacion(), organizacion.getIdOrganizacion());
    }

    public boolean esDonanteResponsable(Usuario usuario, Lote lote) {
        return usuario.getRol() == Rol.DONANTE && perteneceA(usuario, lote.getDonante());
    }

    /** Solo el donante responsable o la coordinación pueden gestionar el lote. */
    public void validarResponsableDelLote(Usuario usuario, Lote lote) {
        if (!esCoordinacion(usuario) && !esDonanteResponsable(usuario, lote)) {
            throw new ForbiddenOperationException(
                    "Solo el donante responsable o un coordinador autorizado pueden gestionar este lote");
        }
    }

    public void validarRol(Usuario usuario, Rol rol) {
        if (usuario.getRol() != rol) {
            throw new ForbiddenOperationException("Operación exclusiva del rol " + rol);
        }
    }

    /** RF02 y RN08: la organización debe estar verificada y sin suspensión para operar. */
    public void validarOrganizacionOperativa(Organizacion organizacion) {
        if (organizacion.isBloqueada()) {
            throw new AccountBlockedException("La organización está suspendida por acumulación de strikes");
        }
        if (!organizacion.isVerificada()) {
            throw new BusinessRuleException("RF02", "La organización aún no ha sido verificada por un administrador");
        }
    }
}
