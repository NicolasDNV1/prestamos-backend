package com.alimentared.backend.service.impl;

import com.alimentared.backend.dto.request.OrganizacionRequest;
import com.alimentared.backend.dto.response.OrganizacionResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import com.alimentared.backend.entity.enums.Rol;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.exception.DuplicateResourceException;
import com.alimentared.backend.exception.ForbiddenOperationException;
import com.alimentared.backend.exception.ResourceNotFoundException;
import com.alimentared.backend.mapper.OrganizacionMapper;
import com.alimentared.backend.repository.OrganizacionRepository;
import com.alimentared.backend.service.AuditoriaService;
import com.alimentared.backend.service.OrganizacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizacionServiceImpl implements OrganizacionService {

    private final OrganizacionRepository organizacionRepository;
    private final OrganizacionMapper organizacionMapper;
    private final AuditoriaService auditoriaService;
    private final AccesoHelper accesoHelper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrganizacionResponse> listarOrganizaciones(Boolean verificada, Pageable pageable) {
        Page<Organizacion> page = verificada == null
                ? organizacionRepository.findAll(pageable)
                : organizacionRepository.findByVerificada(verificada, pageable);
        return PageResponse.from(page.map(organizacionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizacionResponse consultarOrganizacionPorId(Long organizacionId, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Organizacion organizacion = buscar(organizacionId);
        validarPropietarioOCoordinacion(usuario, organizacion);
        return organizacionMapper.toResponse(organizacion);
    }

    @Override
    @Transactional
    public OrganizacionResponse actualizarOrganizacion(Long organizacionId, OrganizacionRequest request, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Organizacion organizacion = buscar(organizacionId);
        if (usuario.getRol() != Rol.ADMINISTRADOR && !accesoHelper.perteneceA(usuario, organizacion)) {
            throw new ForbiddenOperationException("Solo la propia organización o un administrador pueden modificarla");
        }
        String nit = request.nit().trim();
        if (organizacionRepository.existsByNitAndIdOrganizacionNot(nit, organizacionId)) {
            throw new DuplicateResourceException("Ya existe una organización registrada con el NIT " + nit);
        }
        organizacionMapper.updateEntity(organizacion, request);
        return organizacionMapper.toResponse(organizacion);
    }

    @Override
    @Transactional
    public OrganizacionResponse verificarOrganizacion(Long organizacionId, Long usuarioId) {
        Usuario administrador = accesoHelper.usuario(usuarioId);
        Organizacion organizacion = buscar(organizacionId);
        if (organizacion.isVerificada()) {
            throw new BusinessRuleException("RF02", "La organización ya se encuentra verificada");
        }
        organizacion.setVerificada(true);
        auditoriaService.registrar(EntidadAuditada.ORGANIZACION, organizacionId, "PENDIENTE_VERIFICACION",
                "VERIFICADA", administrador, "Organización verificada por el administrador");
        return organizacionMapper.toResponse(organizacion);
    }

    private Organizacion buscar(Long organizacionId) {
        return organizacionRepository.findById(organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización", organizacionId));
    }

    private void validarPropietarioOCoordinacion(Usuario usuario, Organizacion organizacion) {
        if (!accesoHelper.esCoordinacion(usuario) && !accesoHelper.perteneceA(usuario, organizacion)) {
            throw new ForbiddenOperationException("No tiene acceso a la información de esta organización");
        }
    }
}
