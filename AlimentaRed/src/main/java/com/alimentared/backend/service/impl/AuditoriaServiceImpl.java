package com.alimentared.backend.service.impl;

import com.alimentared.backend.dto.response.HistorialEstadoResponse;
import com.alimentared.backend.entity.HistorialEstado;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import com.alimentared.backend.mapper.HistorialEstadoMapper;
import com.alimentared.backend.repository.HistorialEstadoRepository;
import com.alimentared.backend.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final int MAX_JUSTIFICACION = 500;

    private final HistorialEstadoRepository historialEstadoRepository;
    private final HistorialEstadoMapper historialEstadoMapper;
    private final Clock clock;

    @Override
    @Transactional
    public void registrar(EntidadAuditada entidad, Long entidadId, Object estadoAnterior, Object estadoNuevo,
                          Usuario responsable, String justificacion) {
        historialEstadoRepository.save(HistorialEstado.builder()
                .entidad(entidad)
                .entidadId(entidadId)
                .estadoAnterior(estadoAnterior != null ? estadoAnterior.toString() : null)
                .estadoNuevo(estadoNuevo.toString())
                .usuarioResponsable(responsable)
                .fecha(LocalDateTime.now(clock))
                .justificacion(recortar(justificacion))
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> listar(EntidadAuditada entidad, Long entidadId) {
        return historialEstadoRepository.findByEntidadAndEntidadIdOrderByFechaAscIdHistorialAsc(entidad, entidadId)
                .stream()
                .map(historialEstadoMapper::toResponse)
                .toList();
    }

    private static String recortar(String texto) {
        if (texto == null || texto.length() <= MAX_JUSTIFICACION) {
            return texto;
        }
        return texto.substring(0, MAX_JUSTIFICACION);
    }
}
