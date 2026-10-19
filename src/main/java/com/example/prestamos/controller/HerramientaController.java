package com.example.prestamos.controller;

import com.example.prestamos.model.Herramienta;
import com.example.prestamos.service.HerramientaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/herramientas")
@CrossOrigin(origins = "*")
public class HerramientaController {

    @Autowired
    private HerramientaService herramientaService;

    @PostMapping
    public ResponseEntity<Herramienta> registrarHerramienta(@Valid @RequestBody Herramienta herramienta) {
        return ResponseEntity.ok(herramientaService.guardarHerramienta(herramienta));
    }

    @GetMapping
    public ResponseEntity<List<Herramienta>> consultarHerramientas() {
        return ResponseEntity.ok(herramientaService.listarHerramientas());
    }
}