package com.alimentared.backend.service.impl;

import com.alimentared.backend.dto.response.EntregaResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.dto.response.ReporteImpactoResponse;
import com.alimentared.backend.dto.response.ReporteImpactoResponse.ImpactoCategoria;
import com.alimentared.backend.dto.response.ReporteImpactoResponse.OrganizacionStrikes;
import com.alimentared.backend.entity.Asignacion;
import com.alimentared.backend.entity.Entrega;
import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Solicitud;
import com.alimentared.backend.entity.TipoAlimento;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EstadoLote;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.mapper.AsignacionMapper;
import com.alimentared.backend.repository.AsignacionRepository;
import com.alimentared.backend.repository.EntregaRepository;
import com.alimentared.backend.repository.LoteRepository;
import com.alimentared.backend.repository.OrganizacionRepository;
import com.alimentared.backend.repository.SolicitudRepository;
import com.alimentared.backend.repository.StrikeRepository;
import com.alimentared.backend.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alimentared.backend.repository.specification.LoteSpecifications.*;
import static com.alimentared.backend.repository.specification.ReporteSpecifications.*;
import static com.alimentared.backend.repository.specification.SpecificationUtils.todas;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private static final int DIAS_POR_DEFECTO = 30;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final Set<EstadoSolicitud> ESTADOS_RESPONDIDOS =
            Set.of(EstadoSolicitud.APROBADO, EstadoSolicitud.COMPLETADA, EstadoSolicitud.RECHAZADA);

    private final LoteRepository loteRepository;
    private final SolicitudRepository solicitudRepository;
    private final AsignacionRepository asignacionRepository;
    private final EntregaRepository entregaRepository;
    private final StrikeRepository strikeRepository;
    private final OrganizacionRepository organizacionRepository;
    private final AccesoHelper accesoHelper;
    private final AsignacionMapper asignacionMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ReporteImpactoResponse consultarImpacto(LocalDate desde, LocalDate hasta, Long categoriaId) {
        Periodo periodo = resolverPeriodo(desde, hasta);

        List<Lote> lotes = loteRepository.findAll(todas(conRelaciones(),
                registradoEntre(periodo.inicio(), periodo.fin()),
                categoria(categoriaId),
                estadoDistintoDe(EstadoLote.BORRADOR)));
        List<Asignacion> asignaciones = asignacionRepository.findAll(todas(
                asignacionesEntre(periodo.inicio(), periodo.fin()),
                asignacionesDeCategoria(categoriaId)));
        List<Entrega> entregas = entregaRepository.findAll(todas(entregasConRelaciones(),
                entregadasEntre(periodo.inicio(), periodo.fin()),
                entregasDeCategoria(categoriaId)));
        List<Solicitud> solicitudes = solicitudRepository.findAll(todas(
                solicitudesCreadasEntre(periodo.inicio(), periodo.fin()),
                solicitudesDeCategoria(categoriaId)));

        BigDecimal publicada = sumar(lotes, Lote::getCantidadInicial);
        BigDecimal recuperada = sumar(entregas, Entrega::getCantidadRetirada);

        return ReporteImpactoResponse.builder()
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .categoriaId(categoriaId)
                .totalLotesPublicados(lotes.size())
                .cantidadTotalPublicada(publicada)
                .cantidadTotalAsignada(sumar(asignaciones, Asignacion::getCantidadAsignada))
                .cantidadTotalRecuperada(recuperada)
                .porcentajeRecuperacion(porcentaje(recuperada, publicada))
                .totalEntregas(entregas.size())
                .entregasFueraDeFranja(entregas.stream().filter(Entrega::isFueraDeFranja).count())
                .solicitudesPorEstado(contarPorEstado(solicitudes))
                .tiempoPromedioRespuestaMinutos(tiempoPromedioRespuesta(solicitudes))
                .strikesRegistrados(strikeRepository.countByFechaRegistroGreaterThanEqualAndFechaRegistroLessThan(
                        periodo.inicio(), periodo.fin()))
                .organizacionesBloqueadas(organizacionRepository.countByBloqueadaTrue())
                .porCategoria(impactoPorCategoria(lotes, entregas))
                .organizacionesConStrikes(organizacionRepository.findByStrikesVigentesGreaterThanOrderByStrikesVigentesDesc(0)
                        .stream()
                        .map(o -> new OrganizacionStrikes(o.getIdOrganizacion(), o.getNombre(),
                                o.getStrikesVigentes(), o.isBloqueada()))
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EntregaResponse> consultarHistorial(LocalDate desde, LocalDate hasta, Long categoriaId,
                                                            Long usuarioId, Pageable pageable) {
        Periodo periodo = resolverPeriodo(desde, hasta);
        Usuario usuario = accesoHelper.usuario(usuarioId);

        Specification<Entrega> spec = todas(entregasConRelaciones(),
                entregadasEntre(periodo.inicio(), periodo.fin()),
                entregasDeCategoria(categoriaId));
        spec = switch (usuario.getRol()) {
            case DONANTE -> spec.and(entregasDeDonante(accesoHelper.organizacionDe(usuario).getIdOrganizacion()));
            case RECEPTORA -> spec.and(entregasDeReceptora(accesoHelper.organizacionDe(usuario).getIdOrganizacion()));
            case COORDINADOR, ADMINISTRADOR -> spec;
        };
        return PageResponse.from(entregaRepository.findAll(spec, pageable).map(asignacionMapper::toEntregaResponse));
    }

    /** RF09: la fecha de inicio no puede ser posterior a la final. Por defecto, últimos 30 días. */
    private Periodo resolverPeriodo(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now(clock);
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO);
        if (inicio.isAfter(fin)) {
            throw new BusinessRuleException("RF09", "La fecha de inicio no puede ser posterior a la fecha final");
        }
        return new Periodo(inicio, fin, inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());
    }

    private List<ImpactoCategoria> impactoPorCategoria(List<Lote> lotes, List<Entrega> entregas) {
        Map<Long, List<Lote>> lotesPorCategoria = lotes.stream()
                .collect(Collectors.groupingBy(lote -> lote.getCategoria().getIdCategoria()));
        Map<Long, BigDecimal> recuperadoPorCategoria = entregas.stream()
                .collect(Collectors.groupingBy(
                        entrega -> entrega.getSolicitud().getLote().getCategoria().getIdCategoria(),
                        Collectors.reducing(BigDecimal.ZERO, Entrega::getCantidadRetirada, BigDecimal::add)));
        Map<Long, TipoAlimento> categorias = lotes.stream()
                .map(Lote::getCategoria)
                .collect(Collectors.toMap(TipoAlimento::getIdCategoria, Function.identity(), (a, b) -> a));
        entregas.stream()
                .map(entrega -> entrega.getSolicitud().getLote().getCategoria())
                .forEach(categoria -> categorias.putIfAbsent(categoria.getIdCategoria(), categoria));

        return categorias.values().stream()
                .sorted(Comparator.comparing(TipoAlimento::getNombre))
                .map(categoria -> {
                    List<Lote> deLaCategoria = lotesPorCategoria.getOrDefault(categoria.getIdCategoria(), List.of());
                    return new ImpactoCategoria(
                            categoria.getIdCategoria(),
                            categoria.getNombre(),
                            categoria.getUnidadMedida(),
                            deLaCategoria.size(),
                            sumar(deLaCategoria, Lote::getCantidadInicial),
                            recuperadoPorCategoria.getOrDefault(categoria.getIdCategoria(), BigDecimal.ZERO));
                })
                .toList();
    }

    private static Map<EstadoSolicitud, Long> contarPorEstado(List<Solicitud> solicitudes) {
        Map<EstadoSolicitud, Long> conteo = new EnumMap<>(EstadoSolicitud.class);
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            conteo.put(estado, 0L);
        }
        solicitudes.forEach(solicitud -> conteo.merge(solicitud.getEstado(), 1L, Long::sum));
        return conteo;
    }

    private static Double tiempoPromedioRespuesta(List<Solicitud> solicitudes) {
        return solicitudes.stream()
                .filter(solicitud -> solicitud.getFechaResolucion() != null)
                .filter(solicitud -> ESTADOS_RESPONDIDOS.contains(solicitud.getEstado()))
                .mapToLong(solicitud -> Duration.between(solicitud.getFechaCreacion(), solicitud.getFechaResolucion()).toSeconds())
                .average()
                .stream()
                .mapToObj(segundos -> BigDecimal.valueOf(segundos / 60.0).setScale(2, RoundingMode.HALF_UP).doubleValue())
                .findFirst()
                .orElse(null);
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static <T> BigDecimal sumar(Collection<T> elementos, Function<T, BigDecimal> valor) {
        return elementos.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record Periodo(LocalDate desde, LocalDate hasta, LocalDateTime inicio, LocalDateTime fin) {
    }
}
