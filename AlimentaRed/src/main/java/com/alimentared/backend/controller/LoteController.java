package com.alimentared.backend.controller;

import com.alimentared.backend.config.UsuarioPrincipal;
import com.alimentared.backend.dto.request.CancelacionRequest;
import com.alimentared.backend.dto.request.LoteFiltroRequest;
import com.alimentared.backend.dto.request.LoteRequest;
import com.alimentared.backend.dto.response.HistorialEstadoResponse;
import com.alimentared.backend.dto.response.LoteResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.entity.enums.EstadoLote;
import com.alimentared.backend.service.LoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lotes", description = "Publicación y catálogo de lotes de alimentos (RF03, RF04)")
@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @Operation(summary = "Listar y filtrar lotes",
            description = "Filtros combinables por categoría, ubicación, estado y rango de fecha de caducidad. "
                    + "Donantes y receptoras ven solo lotes disponibles y vigentes")
    @GetMapping
    public ResponseEntity<PageResponse<LoteResponse>> listar(
            @ParameterObject LoteFiltroRequest filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaCaducidad") Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(loteService.filtrarLotes(filtro, pageable, principal.getId()));
    }

    @Operation(summary = "Lotes publicados por mi organización")
    @PreAuthorize("hasRole('DONANTE')")
    @GetMapping("/mis-lotes")
    public ResponseEntity<PageResponse<LoteResponse>> misLotes(
            @RequestParam(required = false) EstadoLote estado,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(loteService.listarMisLotes(estado, pageable, principal.getId()));
    }

    @Operation(summary = "Detalle del lote",
            description = "Instrucciones y contacto solo se revelan al donante, a la coordinación o a la receptora asignada")
    @GetMapping("/{id}")
    public ResponseEntity<LoteResponse> obtener(@PathVariable Long id,
                                                @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(loteService.obtenerLote(id, principal.getId()));
    }

    @Operation(summary = "Publicar lote", description = "Organizaciones sin verificar solo pueden guardar borradores")
    @PreAuthorize("hasRole('DONANTE')")
    @PostMapping
    public ResponseEntity<LoteResponse> crear(@Valid @RequestBody LoteRequest request,
                                              @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteService.publicarLote(request, principal.getId()));
    }

    @Operation(summary = "Actualizar lote sin solicitudes ni asignaciones")
    @PreAuthorize("hasRole('DONANTE')")
    @PutMapping("/{id}")
    public ResponseEntity<LoteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody LoteRequest request,
                                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(loteService.actualizarLote(id, request, principal.getId()));
    }

    @Operation(summary = "Publicar un lote guardado como borrador")
    @PreAuthorize("hasRole('DONANTE')")
    @PatchMapping("/{id}/publicar")
    public ResponseEntity<LoteResponse> publicar(@PathVariable Long id,
                                                 @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(loteService.publicarBorrador(id, principal.getId()));
    }

    @Operation(summary = "Cancelar lote", description = "RF08: no aplica a lotes entregados ni con asignaciones activas")
    @PreAuthorize("hasAnyRole('DONANTE','COORDINADOR','ADMINISTRADOR')")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<LoteResponse> cancelar(@PathVariable Long id, @Valid @RequestBody CancelacionRequest request,
                                                 @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(loteService.cancelarLote(id, request, principal.getId()));
    }

    @Operation(summary = "Trazabilidad de estados del lote (RN04)")
    @PreAuthorize("hasAnyRole('DONANTE','COORDINADOR','ADMINISTRADOR')")
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEstadoResponse>> historial(@PathVariable Long id,
                                                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(loteService.obtenerHistorial(id, principal.getId()));
    }
}
