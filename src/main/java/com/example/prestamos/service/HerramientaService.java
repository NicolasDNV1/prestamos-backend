package com.example.prestamos.service;

import com.example.prestamos.model.Herramienta;
import com.example.prestamos.repository.HerramientaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HerramientaService {

    @Autowired
    private HerramientaRepository herramientaRepository;

    public List<Herramienta> listarHerramientas() {
        return herramientaRepository.findAll();
    }

    public Herramienta guardarHerramienta(Herramienta herramienta) {
        if (herramienta.getCantidad() < 0) {
            throw new IllegalArgumentException("No se permiten cantidades negativas.");
        }
        return herramientaRepository.save(herramienta);
    }
}