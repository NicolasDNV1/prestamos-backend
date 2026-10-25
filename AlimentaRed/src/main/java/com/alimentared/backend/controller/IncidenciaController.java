package com.alimentared.backend.controller;

import com.alimentared.backend.config.UsuarioPrincipal;
import com.alimentared.backend.dto.request.IncidenciaRequest;
import com.alimentared.backend.dto.request.ResolverIncidenciaRequest;
import com.alimentared.backend.dto.response.IncidenciaResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.service.IncidenciaService;
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

@Tag(name = "Incidencias", description = "Reporte y gestión de incidencias sobre lotes (RF08)")
@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    @Operation(summary = "Reportar incidencia", description = "Bloquea el lote para nuevas solicitudes")
    @PreAuthorize("hasAnyRole('DONANTE','RECEPTORA','COORDINADOR','ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<IncidenciaResponse> reportar(@Valid @RequestBody IncidenciaRequest request,
                                                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.reportarIncidencia(request, principal.getId()));
    }

    @Operation(summary = "Listar incidencias")
    @PreAuthorize("hasAnyRole('COORDINADOR','ADMINISTRADOR')")
    @GetMapping
    public ResponseEntity<PageResponse<IncidenciaResponse>> listar(
            @RequestParam(required = false) Boolean resuelta,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaReporte", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(incidenciaService.listarIncidencias(resuelta, pageable));
    }

    @Operation(summary = "Incidencias de un lote")
    @PreAuthorize("hasAnyRole('DONANTE','COORDINADOR','ADMINISTRADOR')")
    @GetMapping("/lote/{loteId}")
    public ResponseEntity<List<IncidenciaResponse>> porLote(@PathVariable Long loteId,
                                                            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(incidenciaService.listarPorLote(loteId, principal.getId()));
    }

    @Operation(summary = "Resolver incidencia", description = "Reactiva el lote o lo cancela definitivamente")
    @PreAuthorize("hasAnyRole('COORDINADOR','ADMINISTRADOR')")
    @PatchMapping("/{id}/resolver")
    public ResponseEntity<IncidenciaResponse> resolver(@PathVariable Long id,
                                                       @Valid @RequestBody ResolverIncidenciaRequest request,
                                                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(incidenciaService.resolverIncidencia(id, request, principal.getId()));
    }
}
