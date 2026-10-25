package com.alimentared.backend.service;

import com.alimentared.backend.dto.request.TipoAlimentoRequest;
import com.alimentared.backend.dto.response.TipoAlimentoResponse;

import java.util.List;

public interface TipoAlimentoService {

    TipoAlimentoResponse crearCategoria(TipoAlimentoRequest request);

    List<TipoAlimentoResponse> listarCategorias();

    TipoAlimentoResponse actualizarCategoria(Long id, TipoAlimentoRequest request);

    void eliminarCategoria(Long id);

    boolean validarUnidadMedida(String unidad);
}
