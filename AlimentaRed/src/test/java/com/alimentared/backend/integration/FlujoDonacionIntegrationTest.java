package com.alimentared.backend.integration;

import com.alimentared.backend.service.SolicitudService;
import com.alimentared.backend.support.MutableClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FlujoDonacionIntegrationTest.RelojDePrueba.class)
class FlujoDonacionIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        MutableClock relojDePrueba() {
            return new MutableClock(Instant.now(), ZONA);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MutableClock reloj;
    @Autowired
    private SolicitudService solicitudService;

    private String tokenAdmin;
    private String tokenCoordinador;
    private Long categoriaId;

    @BeforeEach
    void setUp() throws Exception {
        reloj.reiniciar();
        tokenAdmin = login("admin@alimentared.com", "Admin12345");
        tokenCoordinador = login("coordinador@alimentared.com", "Coord12345");
        MvcResult categorias = mockMvc.perform(conToken(get("/api/categorias"), tokenAdmin))
                .andExpect(status().isOk()).andReturn();
        categoriaId = ((Number) JsonPath.read(categorias.getResponse().getContentAsString(), "$[0].id")).longValue();
    }

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"));
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/lotes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void loginConCredencialesInvalidasDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", "admin@alimentared.com", "contrasena", "incorrecta1"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void receptoraNoPuedeConsultarIndicadoresDeCoordinacion() throws Exception {
        Organizacion receptora = registrar("RECEPTORA");
        mockMvc.perform(conToken(get("/api/reportes/impacto"), receptora.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void datosInvalidosYRecursosInexistentesDevuelvenErroresControlados() throws Exception {
        Organizacion donante = registrarVerificada("DONANTE");
        Map<String, Object> loteInvalido = new LinkedHashMap<>(datosLote("-5", "1", "1"));
        loteInvalido.put("fechaCaducidad", LocalDateTime.now(reloj).minusDays(1).toString());

        mockMvc.perform(conToken(post("/api/lotes"), donante.token()).content(json(loteInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(conToken(get("/api/lotes/999999"), donante.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void flujoCompletoPublicarSolicitarAprobarRetirarYRestock() throws Exception {
        Organizacion donante = registrarVerificada("DONANTE");
        Organizacion receptora = registrarVerificada("RECEPTORA");

        Long loteId = publicarLote(donante.token(), "10", "2", "1");

        // RF04: el catálogo oculta instrucciones y contacto antes de la asignación.
        mockMvc.perform(conToken(get("/api/lotes/" + loteId), receptora.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.instruccionesRecogida").doesNotExist());

        // RN10: pedir 9 de 10 dejaría un remanente (1) menor a la unidad mínima (2).
        solicitar(receptora.token(), loteId, "9")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN10"));

        Long solicitudId = idDe(solicitar(receptora.token(), loteId, "4")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE")));

        // Solo el donante responsable o la coordinación resuelven.
        mockMvc.perform(conToken(patch("/api/solicitudes/" + solicitudId + "/resolver"), receptora.token())
                        .content(json(Map.of("aprobada", true))))
                .andExpect(status().isForbidden());

        mockMvc.perform(conToken(patch("/api/solicitudes/" + solicitudId + "/resolver"), donante.token())
                        .content(json(Map.of("aprobada", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        // RN03 y RF06: la disponibilidad baja y se revelan los datos de recogida a la receptora asignada.
        mockMvc.perform(conToken(get("/api/lotes/" + loteId), receptora.token()))
                .andExpect(jsonPath("$.cantidadDisponible").value(closeTo(6.0, 0.0001)))
                .andExpect(jsonPath("$.instruccionesRecogida").exists());

        MvcResult asignaciones = mockMvc.perform(conToken(get("/api/entregas/asignaciones"), receptora.token()))
                .andExpect(status().isOk()).andReturn();
        Long asignacionId = ((Number) JsonPath.read(asignaciones.getResponse().getContentAsString(),
                "$.contenido[0].id")).longValue();

        // Solo el donante responsable (o coordinación) confirma la salida del alimento.
        mockMvc.perform(conToken(patch("/api/entregas/" + asignacionId + "/confirmar"), receptora.token())
                        .content(json(Map.of("cantidadRetirada", "4"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(conToken(patch("/api/entregas/" + asignacionId + "/confirmar"), donante.token())
                        .content(json(Map.of("cantidadRetirada", "3", "observaciones", "Retiro parcial"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PARCIAL"))
                .andExpect(jsonPath("$.fueraDeFranja").value(false));

        // RN11: la coordinación devuelve al catálogo la unidad no retirada.
        mockMvc.perform(conToken(patch("/api/entregas/" + asignacionId + "/restock"), tokenCoordinador)
                        .content(json(Map.of("observaciones", "La fundación no pudo llevar todo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA_CON_RESTOCK"))
                .andExpect(jsonPath("$.cantidadRetirada").value(closeTo(3.0, 0.0001)));

        mockMvc.perform(conToken(get("/api/lotes/" + loteId), donante.token()))
                .andExpect(jsonPath("$.cantidadDisponible").value(closeTo(7.0, 0.0001)))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));

        mockMvc.perform(conToken(get("/api/solicitudes/" + solicitudId), receptora.token()))
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        // RN04: trazabilidad de estados con responsable.
        mockMvc.perform(conToken(get("/api/solicitudes/" + solicitudId + "/historial"), receptora.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1].estadoNuevo").value("APROBADO"))
                .andExpect(jsonPath("$[1].responsable").value("Usuario DONANTE"));

        mockMvc.perform(conToken(get("/api/reportes/impacto"), tokenCoordinador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadTotalRecuperada").value(greaterThanOrEqualTo(3.0)));

        mockMvc.perform(conToken(get("/api/reportes/historial"), receptora.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)));
    }

    @Test
    void nuncaSeAsignaPorEncimaDeLaDisponibilidad() throws Exception {
        Organizacion donante = registrarVerificada("DONANTE");
        Organizacion receptoraA = registrarVerificada("RECEPTORA");
        Organizacion receptoraB = registrarVerificada("RECEPTORA");
        Long loteId = publicarLote(donante.token(), "10", "2", "1");

        Long solicitudA = idDe(solicitar(receptoraA.token(), loteId, "7").andExpect(status().isCreated()));
        Long solicitudB = idDe(solicitar(receptoraB.token(), loteId, "5").andExpect(status().isCreated()));

        mockMvc.perform(conToken(patch("/api/solicitudes/" + solicitudA + "/resolver"), donante.token())
                        .content(json(Map.of("aprobada", true))))
                .andExpect(status().isOk());

        // RN02: quedan 3 y B pidió 5; la asignación se rechaza en el momento exacto de aprobarla.
        mockMvc.perform(conToken(patch("/api/solicitudes/" + solicitudB + "/resolver"), donante.token())
                        .content(json(Map.of("aprobada", true))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN02"));

        mockMvc.perform(conToken(get("/api/lotes/" + loteId), donante.token()))
                .andExpect(jsonPath("$.cantidadDisponible").value(closeTo(3.0, 0.0001)));
    }

    @Test
    void organizacionNoPuedeTenerMasDeTresSolicitudesActivas() throws Exception {
        Organizacion donante = registrarVerificada("DONANTE");
        Organizacion receptora = registrarVerificada("RECEPTORA");
        for (int i = 0; i < 3; i++) {
            Long loteId = publicarLote(donante.token(), "10", "1", "1");
            solicitar(receptora.token(), loteId, "2").andExpect(status().isCreated());
        }
        Long cuartoLote = publicarLote(donante.token(), "10", "1", "1");

        solicitar(receptora.token(), cuartoLote, "2")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN07"));
    }

    @Test
    void ventanaDeEmpateAutoAsignaSinCompetenciaYEsperaDecisionConCompetencia() throws Exception {
        Organizacion donante = registrarVerificada("DONANTE");
        Organizacion receptoraA = registrarVerificada("RECEPTORA");
        Organizacion receptoraB = registrarVerificada("RECEPTORA");
        Long loteSinCompetencia = publicarLote(donante.token(), "10", "1", "1");
        Long loteConCompetencia = publicarLote(donante.token(), "10", "1", "1");

        Long unica = idDe(solicitar(receptoraA.token(), loteSinCompetencia, "3"));
        Long empateA = idDe(solicitar(receptoraA.token(), loteConCompetencia, "3"));
        Long empateB = idDe(solicitar(receptoraB.token(), loteConCompetencia, "4"));

        reloj.avanzar(Duration.ofSeconds(61));
        solicitudService.procesarVentanaEmpate();

        mockMvc.perform(conToken(get("/api/solicitudes/" + unica), receptoraA.token()))
                .andExpect(jsonPath("$.estado").value("APROBADO"));
        mockMvc.perform(conToken(get("/api/solicitudes/" + empateA), receptoraA.token()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.enEmpate").value(true));
        mockMvc.perform(conToken(get("/api/solicitudes/" + empateB), receptoraB.token()))
                .andExpect(jsonPath("$.enEmpate").value(true));
    }

    @Test
    void solicitudPendienteExpiraTrasElSlaDeUnaHora() throws Exception {
        Organizacion donante = registrarVerificada("DONANTE");
        Organizacion receptora = registrarVerificada("RECEPTORA");
        Long loteId = publicarLote(donante.token(), "10", "1", "1");
        Long solicitudId = idDe(solicitar(receptora.token(), loteId, "2"));

        reloj.avanzar(Duration.ofMinutes(61));
        solicitudService.verificarSlaExpiracion();

        mockMvc.perform(conToken(get("/api/solicitudes/" + solicitudId), receptora.token()))
                .andExpect(jsonPath("$.estado").value("EXPIRADA"));
        mockMvc.perform(conToken(get("/api/solicitudes/activas/conteo"), receptora.token()))
                .andExpect(jsonPath("$.activas").value(0));
    }

    @Test
    void tresStrikesBloqueanLaCuentaYLaApelacionLaReactiva() throws Exception {
        Organizacion donante = registrarVerificada("DONANTE");
        Organizacion receptora = registrarVerificada("RECEPTORA");
        Long[] asignaciones = new Long[3];
        for (int i = 0; i < 3; i++) {
            Long loteId = publicarLote(donante.token(), "10", "1", "1");
            Long solicitudId = idDe(solicitar(receptora.token(), loteId, "5"));
            mockMvc.perform(conToken(patch("/api/solicitudes/" + solicitudId + "/resolver"), donante.token())
                            .content(json(Map.of("aprobada", true))))
                    .andExpect(status().isOk());
            asignaciones[i] = idAsignacionDeLote(donante.token(), loteId);
        }

        // RN08: el donante solo puede reportar "no recogida" cuando vence la franja de retiro.
        mockMvc.perform(conToken(patch("/api/entregas/" + asignaciones[0] + "/no-recogida"), donante.token())
                        .content(json(Map.of("observaciones", "Aún no llegan"))))
                .andExpect(status().isUnprocessableEntity());

        reloj.avanzar(Duration.ofHours(25));
        for (Long asignacionId : asignaciones) {
            mockMvc.perform(conToken(patch("/api/entregas/" + asignacionId + "/no-recogida"), donante.token())
                            .content(json(Map.of("observaciones", "La fundación no se presentó"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("NO_RECOGIDA"));
        }

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", receptora.correo(), "contrasena", "Clave12345"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_SUSPENDIDA"));
        mockMvc.perform(conToken(get("/api/usuarios/me"), receptora.token()))
                .andExpect(status().isForbidden());

        MvcResult strikes = mockMvc.perform(conToken(get("/api/organizaciones/" + receptora.id() + "/strikes"), tokenCoordinador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andReturn();
        Long strikeId = ((Number) JsonPath.read(strikes.getResponse().getContentAsString(), "$[0].id")).longValue();

        mockMvc.perform(conToken(patch("/api/organizaciones/" + receptora.id() + "/strikes/apelar"), tokenCoordinador)
                        .content(json(Map.of("strikeId", strikeId, "justificacion", "Cierre vial documentado por la alcaldía",
                                "aprobada", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(false));

        login(receptora.correo(), "Clave12345");
    }

    // ---------- utilidades ----------

    private record Organizacion(Long id, String correo, String token) {
    }

    private Organizacion registrarVerificada(String rol) throws Exception {
        Organizacion organizacion = registrar(rol);
        mockMvc.perform(conToken(patch("/api/organizaciones/" + organizacion.id() + "/verificar"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificada").value(true));
        return organizacion;
    }

    private Organizacion registrar(String rol) throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String nit = String.valueOf(100_000_000L + Math.abs(UUID.randomUUID().getMostSignificantBits() % 899_999_999L));
        String correo = rol.toLowerCase() + "-" + sufijo + "@prueba.com";
        Map<String, Object> body = Map.of(
                "nombre", "Usuario " + rol,
                "correo", correo,
                "contrasena", "Clave12345",
                "rol", rol,
                "organizacion", Map.of(
                        "nit", nit,
                        "nombre", "Organización " + sufijo,
                        "ubicacion", "Chía, Cundinamarca",
                        "correo", "org-" + sufijo + "@prueba.com",
                        "telefono", "3001234567"));
        MvcResult resultado = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.organizacion.verificada").value(false))
                .andReturn();
        String respuesta = resultado.getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(respuesta, "$.usuario.organizacion.id")).longValue();
        return new Organizacion(id, correo, JsonPath.read(respuesta, "$.token"));
    }

    private Long publicarLote(String token, String cantidad, String minima, String multiplo) throws Exception {
        return idDe(mockMvc.perform(conToken(post("/api/lotes"), token).content(json(datosLote(cantidad, minima, multiplo))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE")));
    }

    private Map<String, Object> datosLote(String cantidad, String minima, String multiplo) {
        return Map.of(
                "nombre", "Pan artesanal",
                "descripcion", "Pan del día empacado en bolsas de papel",
                "categoriaId", categoriaId,
                "cantidad", cantidad,
                "unidadMinima", minima,
                "multiplo", multiplo,
                "estadoAlimento", "FRESCO",
                "fechaCaducidad", LocalDateTime.now(reloj).plusDays(3).withNano(0).toString(),
                "instruccionesRecogida", "Puerta trasera, preguntar por Ana",
                "contacto", "3109876543");
    }

    private ResultActions solicitar(String token, Long loteId, String cantidad) throws Exception {
        return mockMvc.perform(conToken(post("/api/solicitudes"), token)
                .content(json(Map.of("loteId", loteId, "cantidadSolicitada", cantidad))));
    }

    private Long idAsignacionDeLote(String tokenDonante, Long loteId) throws Exception {
        MvcResult resultado = mockMvc.perform(conToken(get("/api/entregas/asignaciones?size=100"), tokenDonante))
                .andExpect(jsonPath("$.contenido", not(empty())))
                .andReturn();
        return ((Number) JsonPath.<java.util.List<Object>>read(resultado.getResponse().getContentAsString(),
                "$.contenido[?(@.loteId == " + loteId + ")].id").getFirst()).longValue();
    }

    private String login(String correo, String contrasena) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("correo", correo, "contrasena", contrasena))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }

    private Long idDe(ResultActions acciones) throws Exception {
        return ((Number) JsonPath.read(acciones.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String json(Object valor) throws Exception {
        return objectMapper.writeValueAsString(valor);
    }
}
