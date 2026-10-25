package com.alimentared.backend.service;

import com.alimentared.backend.dto.request.OrganizacionRequest;
import com.alimentared.backend.dto.response.OrganizacionResponse;
import com.alimentared.backend.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrganizacionService {

    PageResponse<OrganizacionResponse> listarOrganizaciones(Boolean verificada, Pageable pageable);

    OrganizacionResponse consultarOrganizacionPorId(Long organizacionId, Long usuarioId);

    OrganizacionResponse actualizarOrganizacion(Long organizacionId, OrganizacionRequest request, Long usuarioId);

    OrganizacionResponse verificarOrganizacion(Long organizacionId, Long usuarioId);
}
