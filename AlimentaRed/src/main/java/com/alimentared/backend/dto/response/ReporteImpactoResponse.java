package com.alimentared.backend.dto.response;

import com.alimentared.backend.entity.enums.EstadoSolicitud;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** RF09: indicadores agregados de impacto por periodo y categoría. */
@Builder
public record ReporteImpactoResponse(
        LocalDate desde,
        LocalDate hasta,
        Long categoriaId,
        long totalLotesPublicados,
        BigDecimal cantidadTotalPublicada,
        BigDecimal cantidadTotalAsignada,
        BigDecimal cantidadTotalRecuperada,
        BigDecimal porcentajeRecuperacion,
        long totalEntregas,
        long entregasFueraDeFranja,
        Map<EstadoSolicitud, Long> solicitudesPorEstado,
        Double tiempoPromedioRespuestaMinutos,
        long strikesRegistrados,
        long organizacionesBloqueadas,
        List<ImpactoCategoria> porCategoria,
        List<OrganizacionStrikes> organizacionesConStrikes
) {

    public record ImpactoCategoria(
            Long categoriaId,
            String categoria,
            String unidadMedida,
            long lotes,
            BigDecimal cantidadPublicada,
            BigDecimal cantidadRecuperada
    ) {
    }

    public record OrganizacionStrikes(
            Long organizacionId,
            String nombre,
            int strikesVigentes,
            boolean bloqueada
    ) {
    }
}
