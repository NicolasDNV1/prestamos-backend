package com.alimentared.backend.service.impl;

import com.alimentared.backend.config.AlimentaRedProperties;
import com.alimentared.backend.dto.request.SolicitudRequest;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.Solicitud;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import com.alimentared.backend.entity.enums.Rol;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.mapper.SolicitudMapper;
import com.alimentared.backend.repository.AsignacionRepository;
import com.alimentared.backend.repository.LoteRepository;
import com.alimentared.backend.repository.OrganizacionRepository;
import com.alimentared.backend.repository.SolicitudRepository;
import com.alimentared.backend.repository.UsuarioRepository;
import com.alimentared.backend.service.AuditoriaService;
import com.alimentared.backend.service.InventarioService;
import com.alimentared.backend.service.StrikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 25, 10, 0);

    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private OrganizacionRepository organizacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private StrikeService strikeService;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private SolicitudMapper solicitudMapper;
    @Mock
    private TransactionTemplate transactionTemplate;

    private SolicitudServiceImpl service;

    @BeforeEach
    void setUp() {
        AlimentaRedProperties properties = new AlimentaRedProperties("America/Bogota",
                new AlimentaRedProperties.Jwt("secreto", 60),
                new AlimentaRedProperties.Reglas(3, 3, 60, 60, 24, 4),
                new AlimentaRedProperties.Seed(false, null, null, null, null));
        Clock clock = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        service = new SolicitudServiceImpl(solicitudRepository, asignacionRepository, loteRepository,
                organizacionRepository, inventarioService, strikeService, auditoriaService,
                new AccesoHelper(usuarioRepository), solicitudMapper, properties, transactionTemplate, clock);
    }

    @Test
    void rechazaNuevaSolicitudCuandoLaOrganizacionTieneTresActivas() {
        Organizacion organizacion = Organizacion.builder().idOrganizacion(10L).nombre("Comedor").verificada(true).build();
        Usuario receptora = Usuario.builder().idUsuario(1L).rol(Rol.RECEPTORA).organizacion(organizacion).build();
        when(usuarioRepository.findWithOrganizacionByIdUsuario(1L)).thenReturn(Optional.of(receptora));
        when(solicitudRepository.countByOrganizacion_IdOrganizacionAndEstadoIn(10L, EstadoSolicitud.ACTIVOS))
                .thenReturn(3L);

        assertThatThrownBy(() -> service.crearSolicitud(new SolicitudRequest(5L, new BigDecimal("2")), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("codigo")
                .isEqualTo("RN07");
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void expiraSolicitudesPendientesQueSuperanElSla() {
        Solicitud vencida = Solicitud.builder()
                .idSolicitud(7L)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCreacion(AHORA.minusMinutes(61))
                .build();
        when(solicitudRepository.findByEstadoAndFechaCreacionBefore(EstadoSolicitud.PENDIENTE, AHORA.minusMinutes(60)))
                .thenReturn(List.of(vencida));

        service.verificarSlaExpiracion();

        assertThat(vencida.getEstado()).isEqualTo(EstadoSolicitud.EXPIRADA);
        assertThat(vencida.getFechaResolucion()).isEqualTo(AHORA);
        verify(auditoriaService).registrar(eq(EntidadAuditada.SOLICITUD), eq(7L), eq(EstadoSolicitud.PENDIENTE),
                eq(EstadoSolicitud.EXPIRADA), isNull(), anyString());
    }
}
