package com.alimentared.backend.controller;

import com.alimentared.backend.config.UsuarioPrincipal;
import com.alimentared.backend.dto.request.ConfirmarEntregaRequest;
import com.alimentared.backend.dto.request.NovedadEntregaRequest;
import com.alimentared.backend.dto.response.AsignacionResponse;
import com.alimentared.backend.dto.response.EntregaResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.service.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Entregas", description = "Asignaciones, retiros parciales/totales y restock (RF07, RN08, RN11)")
@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @Operation(summary = "Confirmar retiro (total o parcial) de una asignación",
            description = "Solo el donante responsable o la coordinación. Fuera de franja genera strike (RF07)")
    @PreAuthorize("hasAnyRole('DONANTE','COORDINADOR','ADMINISTRADOR')")
    @PatchMapping("/{asignacionId}/confirmar")
    public ResponseEntity<EntregaResponse> confirmar(@PathVariable Long asignacionId,
                                                     @Valid @RequestBody ConfirmarEntregaRequest request,
                                                     @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(entregaService.confirmarRecepcion(asignacionId, request, principal.getId()));
    }

    @Operation(summary = "Reportar asignación no recogida",
            description = "RN08: repone el inventario pendiente y suma un strike a la receptora")
    @PreAuthorize("hasAnyRole('DONANTE','COORDINADOR','ADMINISTRADOR')")
    @PatchMapping("/{asignacionId}/no-recogida")
    public ResponseEntity<AsignacionResponse> noRecogida(@PathVariable Long asignacionId,
                                                         @Valid @RequestBody NovedadEntregaRequest request,
                                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(entregaService.registrarNoRecogida(asignacionId, request, principal.getId()));
    }

    @Operation(summary = "Restock del sobrante no retirado", description = "RN11: devuelve la cantidad pendiente al catálogo")
    @PreAuthorize("hasAnyRole('COORDINADOR','ADMINISTRADOR')")
    @PatchMapping("/{asignacionId}/restock")
    public ResponseEntity<AsignacionResponse> restock(@PathVariable Long asignacionId,
                                                      @Valid @RequestBody NovedadEntregaRequest request,
                                                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(entregaService.restockSobrante(asignacionId, request, principal.getId()));
    }

    @Operation(summary = "Listar asignaciones visibles para el usuario",
            description = "Receptora: las suyas. Donante: las de sus lotes. Coordinación: todas")
    @GetMapping("/asignaciones")
    public ResponseEntity<PageResponse<AsignacionResponse>> asignaciones(
            @ParameterObject @PageableDefault(size = 20, sort = "fechaAsignacion", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(entregaService.listarAsignaciones(principal.getId(), pageable));
    }

    @Operation(summary = "Detalle de la asignación con instrucciones de recogida")
    @GetMapping("/asignaciones/{asignacionId}")
    public ResponseEntity<AsignacionResponse> asignacion(@PathVariable Long asignacionId,
                                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(entregaService.obtenerAsignacion(asignacionId, principal.getId()));
    }

    @Operation(summary = "Retiros registrados para una asignación")
    @GetMapping("/asignaciones/{asignacionId}/retiros")
    public ResponseEntity<List<EntregaResponse>> retiros(@PathVariable Long asignacionId,
                                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(entregaService.listarEntregas(asignacionId, principal.getId()));
    }
}
