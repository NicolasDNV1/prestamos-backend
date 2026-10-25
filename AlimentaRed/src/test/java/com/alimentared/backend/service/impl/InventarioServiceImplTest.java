package com.alimentared.backend.service.impl;

import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import com.alimentared.backend.entity.enums.EstadoAlimento;
import com.alimentared.backend.entity.enums.EstadoLote;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.repository.AsignacionRepository;
import com.alimentared.backend.repository.LoteRepository;
import com.alimentared.backend.service.AuditoriaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 25, 10, 0);

    @Mock
    private LoteRepository loteRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private EntityManager entityManager;

    private InventarioServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        service = new InventarioServiceImpl(loteRepository, asignacionRepository, auditoriaService, entityManager, clock);
    }

    @Test
    void rechazaCantidadSuperiorALaDisponible() {
        assertRegla(() -> service.validarFraccionamiento(lote("10", "2", "1"), bd("10.5")), "RN02");
    }

    @Test
    void rechazaCantidadMenorALaUnidadMinima() {
        assertRegla(() -> service.validarFraccionamiento(lote("10", "2", "1"), bd("1")), "RN05");
    }

    @Test
    void rechazaCantidadQueNoEsMultiplo() {
        assertRegla(() -> service.validarFraccionamiento(lote("20", "5", "5"), bd("7")), "RN10");
    }

    @Test
    void rechazaSolicitudQueDejaRemanenteInutil() {
        assertRegla(() -> service.validarFraccionamiento(lote("10", "2", "1"), bd("9")), "RN10");
    }

    @Test
    void permiteFraccionValidaYRetiroTotalDelRemanente() {
        assertThatCode(() -> service.validarFraccionamiento(lote("10", "2", "1"), bd("4"))).doesNotThrowAnyException();
        assertThatCode(() -> service.validarFraccionamiento(lote("23", "5", "5"), bd("23"))).doesNotThrowAnyException();
        assertThatCode(() -> service.validarFraccionamiento(lote("2.5", "0.5", "0.25"), bd("1.75"))).doesNotThrowAnyException();
    }

    @Test
    void rechazaLoteVencido() {
        Lote lote = lote("10", "1", "1");
        lote.setFechaCaducidad(AHORA.minusMinutes(1));
        assertRegla(() -> service.validarLoteSolicitable(lote, AHORA), "RN01");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoLote.class, names = {"CANCELADO", "ENTREGADO", "VENCIDO", "INCIDENCIA"})
    void rechazaLotesNoSolicitables(EstadoLote estado) {
        Lote lote = lote("10", "1", "1");
        lote.setEstado(estado);
        assertRegla(() -> service.validarLoteSolicitable(lote, AHORA), "RN01");
    }

    @Test
    void descontarTodoElInventarioMarcaElLoteComoAsignado() {
        Lote lote = lote("4", "1", "1");
        service.descontar(lote, bd("4"), null);

        assertThat(lote.getCantidadDisponible()).isEqualByComparingTo("0");
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.ASIGNADO);
        verify(auditoriaService).registrar(eq(EntidadAuditada.LOTE), eq(1L), eq(EstadoLote.DISPONIBLE),
                eq(EstadoLote.ASIGNADO), any(), any());
    }

    @Test
    void restockDevuelveElLoteAlCatalogo() {
        Lote lote = lote("0", "1", "1");
        lote.setEstado(EstadoLote.ASIGNADO);

        service.reponer(lote, bd("3"), null, "RN11");

        assertThat(lote.getCantidadDisponible()).isEqualByComparingTo("3");
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.DISPONIBLE);
    }

    @Test
    void restockNoPuedeSuperarLaCantidadInicial() {
        assertRegla(() -> service.reponer(lote("9", "1", "1"), bd("2"), null, "RN11"), "RN02");
    }

    private Lote lote(String disponible, String minima, String multiplo) {
        return Lote.builder()
                .idLote(1L)
                .cantidadInicial(bd(disponible).max(bd("10")))
                .cantidadDisponible(bd(disponible))
                .unidadMinima(bd(minima))
                .multiplo(bd(multiplo))
                .estado(EstadoLote.DISPONIBLE)
                .estadoAlimento(EstadoAlimento.FRESCO)
                .fechaCaducidad(AHORA.plusDays(1))
                .build();
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private static void assertRegla(org.assertj.core.api.ThrowableAssert.ThrowingCallable accion, String codigo) {
        assertThatThrownBy(accion)
                .isInstanceOf(BusinessRuleException.class)
                .extracting("codigo")
                .isEqualTo(codigo);
    }
}
