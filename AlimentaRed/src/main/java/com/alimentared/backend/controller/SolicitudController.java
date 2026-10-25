package com.alimentared.backend.controller;

import com.alimentared.backend.config.UsuarioPrincipal;
import com.alimentared.backend.dto.request.CancelacionRequest;
import com.alimentared.backend.dto.request.ResolverSolicitudRequest;
import com.alimentared.backend.dto.request.SolicitudRequest;
import com.alimentared.backend.dto.response.HistorialEstadoResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.dto.response.SolicitudResponse;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import com.alimentared.backend.service.SolicitudService;
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
import java.util.Map;

@Tag(name = "Solicitudes", description = "Solicitudes de donación y asignación (RF05, RF06, RF08)")
@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    @Operation(summary = "Solicitar un lote",
            description = "Valida RN01 (estado/caducidad), RN05/RN10 (fraccionamiento) y RN07 (máximo 3 activas)")
    @PreAuthorize("hasRole('RECEPTORA')")
    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(@Valid @RequestBody SolicitudRequest request,
                                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crearSolicitud(request, principal.getId()));
    }

    @Operation(summary = "Listar todas las solicitudes")
    @PreAuthorize("hasAnyRole('COORDINADOR','ADMINISTRADOR')")
    @GetMapping
    public ResponseEntity<PageResponse<SolicitudResponse>> listar(
            @RequestParam(required = false) EstadoSolicitud estado,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(solicitudService.listarSolicitudes(estado, pageable));
    }

    @Operation(summary = "Solicitudes de mi organización")
    @PreAuthorize("hasRole('RECEPTORA')")
    @GetMapping("/mis-solicitudes")
    public ResponseEntity<PageResponse<SolicitudResponse>> misSolicitudes(
            @ParameterObject @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudService.obtenerSolicitudesPorOrganizacion(principal.getId(), pageable));
    }

    @Operation(summary = "Conteo de solicitudes activas de mi organización (RN07)")
    @PreAuthorize("hasRole('RECEPTORA')")
    @GetMapping("/activas/conteo")
    public ResponseEntity<Map<String, Long>> contarActivas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(Map.of("activas", solicitudService.contarSolicitudesActivas(principal.getId())));
    }

    @Operation(summary = "Solicitudes recibidas por un lote", description = "Incluye las marcadas en empate (RN09)")
    @PreAuthorize("hasAnyRole('DONANTE','COORDINADOR','ADMINISTRADOR')")
    @GetMapping("/lote/{loteId}")
    public ResponseEntity<PageResponse<SolicitudResponse>> porLote(
            @PathVariable Long loteId,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudService.listarPorLote(loteId, principal.getId(), pageable));
    }

    @Operation(summary = "Detalle de la solicitud")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponse> obtener(@PathVariable Long id,
                                                     @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudService.obtenerSolicitud(id, principal.getId()));
    }

    @Operation(summary = "Aprobar o rechazar solicitud",
            description = "Al aprobar se descuenta el inventario y se crea la asignación (RN02, RN03)")
    @PreAuthorize("hasAnyRole('DONANTE','COORDINADOR','ADMINISTRADOR')")
    @PatchMapping("/{id}/resolver")
    public ResponseEntity<SolicitudResponse> resolver(@PathVariable Long id,
                                                      @Valid @RequestBody ResolverSolicitudRequest request,
                                                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudService.resolverSolicitud(id, request, principal.getId()));
    }

    @Operation(summary = "Cancelar solicitud",
            description = "Si ya estaba asignada se repone el inventario y la receptora recibe un strike (RN12)")
    @PreAuthorize("hasAnyRole('RECEPTORA','COORDINADOR','ADMINISTRADOR')")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SolicitudResponse> cancelar(@PathVariable Long id,
                                                      @Valid @RequestBody CancelacionRequest request,
                                                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudService.cancelarSolicitud(id, request, principal.getId()));
    }

    @Operation(summary = "Trazabilidad de estados de la solicitud (RN04)")
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEstadoResponse>> historial(@PathVariable Long id,
                                                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudService.obtenerHistorial(id, principal.getId()));
    }
}
