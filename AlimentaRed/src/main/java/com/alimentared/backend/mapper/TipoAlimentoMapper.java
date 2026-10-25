package com.alimentared.backend.mapper;

import com.alimentared.backend.dto.request.TipoAlimentoRequest;
import com.alimentared.backend.dto.response.TipoAlimentoResponse;
import com.alimentared.backend.entity.TipoAlimento;
import org.springframework.stereotype.Component;

@Component
public class TipoAlimentoMapper {

    public TipoAlimento toEntity(TipoAlimentoRequest request) {
        TipoAlimento tipo = new TipoAlimento();
        updateEntity(tipo, request);
        return tipo;
    }

    public void updateEntity(TipoAlimento tipo, TipoAlimentoRequest request) {
        tipo.setNombre(request.nombre().trim());
        tipo.setDescripcion(request.descripcion());
        tipo.setUnidadMedida(request.unidadMedida().trim().toUpperCase());
    }

    public TipoAlimentoResponse toResponse(TipoAlimento tipo) {
        return new TipoAlimentoResponse(tipo.getIdCategoria(), tipo.getNombre(),
                tipo.getDescripcion(), tipo.getUnidadMedida());
    }
}
