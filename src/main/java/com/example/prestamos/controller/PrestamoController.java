package com.example.prestamos.controller;

import com.example.prestamos.dto.PrestamoRequest;
import com.example.prestamos.model.Prestamo;
import com.example.prestamos.service.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
@CrossOrigin(origins = "*")
public class PrestamoController {

    @Autowired
    private PrestamoService prestamoService;

    @PostMapping
    public ResponseEntity<Object> registrarPrestamo(@Valid @RequestBody PrestamoRequest request) {
        try {
            Prestamo nuevoPrestamo = prestamoService.registrarPrestamo(request);
            return ResponseEntity.ok(nuevoPrestamo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Prestamo>> consultarTodosLosPrestamos() {
        return ResponseEntity.ok(prestamoService.listarPrestamos());
    }

    @GetMapping("/herramienta/{id}")
    public ResponseEntity<List<Prestamo>> consultarPrestamosPorHerramienta(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.listarPrestamosPorHerramienta(id));
    }
}