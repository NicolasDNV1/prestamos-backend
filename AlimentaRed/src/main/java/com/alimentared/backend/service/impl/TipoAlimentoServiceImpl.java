package com.alimentared.backend.service.impl;

import com.alimentared.backend.dto.request.TipoAlimentoRequest;
import com.alimentared.backend.dto.response.TipoAlimentoResponse;
import com.alimentared.backend.entity.TipoAlimento;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.exception.DuplicateResourceException;
import com.alimentared.backend.exception.ResourceNotFoundException;
import com.alimentared.backend.mapper.TipoAlimentoMapper;
import com.alimentared.backend.repository.LoteRepository;
import com.alimentared.backend.repository.TipoAlimentoRepository;
import com.alimentared.backend.service.TipoAlimentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TipoAlimentoServiceImpl implements TipoAlimentoService {

    private static final Set<String> UNIDADES_PERMITIDAS =
            Set.of("KG", "G", "L", "ML", "UNIDAD", "CAJA", "PAQUETE", "PORCION", "BOLSA");

    private final TipoAlimentoRepository tipoAlimentoRepository;
    private final LoteRepository loteRepository;
    private final TipoAlimentoMapper tipoAlimentoMapper;

    @Override
    @Transactional
    public TipoAlimentoResponse crearCategoria(TipoAlimentoRequest request) {
        validarUnidad(request.unidadMedida());
        if (tipoAlimentoRepository.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new DuplicateResourceException("Ya existe una categoría con el nombre " + request.nombre());
        }
        TipoAlimento tipo = tipoAlimentoRepository.save(tipoAlimentoMapper.toEntity(request));
        return tipoAlimentoMapper.toResponse(tipo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoAlimentoResponse> listarCategorias() {
        return tipoAlimentoRepository.findAll(Sort.by("nombre")).stream()
                .map(tipoAlimentoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TipoAlimentoResponse actualizarCategoria(Long id, TipoAlimentoRequest request) {
        TipoAlimento tipo = buscar(id);
        validarUnidad(request.unidadMedida());
        if (tipoAlimentoRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(request.nombre().trim(), id)) {
            throw new DuplicateResourceException("Ya existe una categoría con el nombre " + request.nombre());
        }
        tipoAlimentoMapper.updateEntity(tipo, request);
        return tipoAlimentoMapper.toResponse(tipo);
    }

    @Override
    @Transactional
    public void eliminarCategoria(Long id) {
        TipoAlimento tipo = buscar(id);
        if (loteRepository.existsByCategoria_IdCategoria(id)) {
            throw new BusinessRuleException("La categoría tiene lotes asociados y no puede eliminarse");
        }
        tipoAlimentoRepository.delete(tipo);
    }

    @Override
    public boolean validarUnidadMedida(String unidad) {
        return unidad != null && UNIDADES_PERMITIDAS.contains(unidad.trim().toUpperCase());
    }

    private void validarUnidad(String unidad) {
        if (!validarUnidadMedida(unidad)) {
            throw new BusinessRuleException("Unidad de medida no permitida. Valores válidos: " + UNIDADES_PERMITIDAS);
        }
    }

    private TipoAlimento buscar(Long id) {
        return tipoAlimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }
}
