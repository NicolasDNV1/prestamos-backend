package com.alimentared.backend.controller;

import com.alimentared.backend.dto.request.TipoAlimentoRequest;
import com.alimentared.backend.dto.response.TipoAlimentoResponse;
import com.alimentared.backend.service.TipoAlimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categorías", description = "Tipos de alimento y unidades de medida")
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class TipoAlimentoController {

    private final TipoAlimentoService tipoAlimentoService;

    @Operation(summary = "Listar categorías de alimentos")
    @GetMapping
    public ResponseEntity<List<TipoAlimentoResponse>> listar() {
        return ResponseEntity.ok(tipoAlimentoService.listarCategorias());
    }

    @Operation(summary = "Crear categoría")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<TipoAlimentoResponse> crear(@Valid @RequestBody TipoAlimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoAlimentoService.crearCategoria(request));
    }

    @Operation(summary = "Actualizar categoría")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoAlimentoResponse> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody TipoAlimentoRequest request) {
        return ResponseEntity.ok(tipoAlimentoService.actualizarCategoria(id, request));
    }

    @Operation(summary = "Eliminar categoría sin lotes asociados")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tipoAlimentoService.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
