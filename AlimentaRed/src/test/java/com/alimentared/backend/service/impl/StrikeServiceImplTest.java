package com.alimentared.backend.service.impl;

import com.alimentared.backend.config.AlimentaRedProperties;
import com.alimentared.backend.dto.request.ApelacionStrikeRequest;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.Solicitud;
import com.alimentared.backend.entity.Strike;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import com.alimentared.backend.entity.enums.Rol;
import com.alimentared.backend.mapper.OrganizacionMapper;
import com.alimentared.backend.repository.OrganizacionRepository;
import com.alimentared.backend.repository.SolicitudRepository;
import com.alimentared.backend.repository.StrikeRepository;
import com.alimentared.backend.repository.UsuarioRepository;
import com.alimentared.backend.service.AuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrikeServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 25, 10, 0);

    @Mock
    private StrikeRepository strikeRepository;
    @Mock
    private OrganizacionRepository organizacionRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private StrikeServiceImpl service;

    @BeforeEach
    void setUp() {
        AlimentaRedProperties properties = new AlimentaRedProperties("America/Bogota",
                new AlimentaRedProperties.Jwt("secreto", 60),
                new AlimentaRedProperties.Reglas(3, 3, 60, 60, 24, 4),
                new AlimentaRedProperties.Seed(false, null, null, null, null));
        service = new StrikeServiceImpl(strikeRepository, organizacionRepository, solicitudRepository,
                auditoriaService, new OrganizacionMapper(), new AccesoHelper(usuarioRepository), properties,
                Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA));
    }

    @Test
    void elTercerStrikeBloqueaLaCuentaYCancelaSusPendientes() {
        Organizacion organizacion = Organizacion.builder().idOrganizacion(10L).strikesVigentes(2).build();
        Solicitud pendiente = Solicitud.builder().idSolicitud(3L).estado(EstadoSolicitud.PENDIENTE).build();
        when(strikeRepository.save(any(Strike.class))).thenAnswer(inv -> inv.getArgument(0));
        when(solicitudRepository.findByOrganizacion_IdOrganizacionAndEstado(10L, EstadoSolicitud.PENDIENTE))
                .thenReturn(List.of(pendiente));

        service.registrarStrike(organizacion, null, "Asignación no recogida", "No se presentó", null);

        assertThat(organizacion.getStrikesVigentes()).isEqualTo(3);
        assertThat(organizacion.isBloqueada()).isTrue();
        assertThat(pendiente.getEstado()).isEqualTo(EstadoSolicitud.CANCELADO);
    }

    @Test
    void apelacionAprobadaAnulaElStrikeYReactivaLaCuenta() {
        Organizacion organizacion = Organizacion.builder()
                .idOrganizacion(10L).nombre("Fundación").strikesVigentes(3).bloqueada(true).build();
        Strike strike = Strike.builder().idStrike(5L).organizacion(organizacion).nombreStrike("No recogida")
                .fechaRegistro(AHORA.minusDays(1)).build();
        Usuario coordinador = Usuario.builder().idUsuario(2L).rol(Rol.COORDINADOR).nombre("Coord").build();
        when(usuarioRepository.findWithOrganizacionByIdUsuario(2L)).thenReturn(Optional.of(coordinador));
        when(organizacionRepository.findById(10L)).thenReturn(Optional.of(organizacion));
        when(strikeRepository.findById(5L)).thenReturn(Optional.of(strike));

        service.apelarStrike(10L, new ApelacionStrikeRequest(5L, "Accidente de tránsito documentado", true), 2L);

        assertThat(strike.isVigente()).isFalse();
        assertThat(strike.getFechaApelacion()).isEqualTo(AHORA);
        assertThat(organizacion.getStrikesVigentes()).isEqualTo(2);
        assertThat(organizacion.isBloqueada()).isFalse();
    }
}
