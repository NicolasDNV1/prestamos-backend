package com.alimentared.backend.mapper;

import com.alimentared.backend.config.AlimentaRedProperties;
import com.alimentared.backend.dto.request.LoteRequest;
import com.alimentared.backend.dto.response.LoteResponse;
import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.TipoAlimento;
import com.alimentared.backend.entity.enums.EstadoLote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LoteMapper {

    private final AlimentaRedProperties properties;
    private final Clock clock;

    public Lote toEntity(LoteRequest request, TipoAlimento categoria, Organizacion donante) {
        Lote lote = Lote.builder().donante(donante).build();
        updateEntity(lote, request, categoria);
        return lote;
    }

    public void updateEntity(Lote lote, LoteRequest request, TipoAlimento categoria) {
        Organizacion donante = lote.getDonante();
        lote.setNombre(request.nombre().trim());
        lote.setDescripcion(request.descripcion().trim());
        lote.setCategoria(categoria);
        lote.setCantidadInicial(request.cantidad());
        lote.setCantidadDisponible(request.cantidad());
        lote.setUnidadMinima(request.unidadMinima());
        lote.setMultiplo(request.multiplo());
        lote.setEstadoAlimento(request.estadoAlimento());
        lote.setFechaCaducidad(request.fechaCaducidad());
        lote.setUbicacion(valorODefecto(request.ubicacion(), donante.getUbicacion()));
        lote.setInstruccionesRecogida(request.instruccionesRecogida());
        lote.setContacto(valorODefecto(request.contacto(),
                valorODefecto(donante.getTelefono(), donante.getCorreo())));
    }

    /** Vista de catálogo: oculta instrucciones y contacto (RF04). */
    public LoteResponse toCatalogResponse(Lote lote) {
        return toResponse(lote, false);
    }

    public LoteResponse toResponse(Lote lote, boolean incluirDatosRecogida) {
        TipoAlimento categoria = lote.getCategoria();
        Organizacion donante = lote.getDonante();
        return LoteResponse.builder()
                .id(lote.getIdLote())
                .nombre(lote.getNombre())
                .descripcion(lote.getDescripcion())
                .categoriaId(categoria.getIdCategoria())
                .categoriaNombre(categoria.getNombre())
                .unidadMedida(categoria.getUnidadMedida())
                .donanteId(donante.getIdOrganizacion())
                .donanteNombre(donante.getNombre())
                .cantidadInicial(lote.getCantidadInicial())
                .cantidadDisponible(lote.getCantidadDisponible())
                .unidadMinima(lote.getUnidadMinima())
                .multiplo(lote.getMultiplo())
                .estado(lote.getEstado())
                .estadoAlimento(lote.getEstadoAlimento())
                .ubicacion(lote.getUbicacion())
                .fechaRegistro(lote.getFechaRegistro())
                .fechaCaducidad(lote.getFechaCaducidad())
                .rescateUrgente(esRescateUrgente(lote))
                .instruccionesRecogida(incluirDatosRecogida ? valorODefecto(lote.getInstruccionesRecogida(), "") : null)
                .contacto(incluirDatosRecogida ? valorODefecto(lote.getContacto(), "") : null)
                .build();
    }

    /** RF03: alerta de "Rescate Urgente" si caduca dentro de la ventana configurada (4 h). */
    private boolean esRescateUrgente(Lote lote) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return lote.getEstado() == EstadoLote.DISPONIBLE
                && !lote.estaVencido(ahora)
                && lote.getFechaCaducidad().isBefore(ahora.plusHours(properties.reglas().horasRescateUrgente()));
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }
}
