package com.example.prestamos.service;

import com.example.prestamos.dto.PrestamoRequest;
import com.example.prestamos.model.Herramienta;
import com.example.prestamos.model.Prestamo;
import com.example.prestamos.repository.HerramientaRepository;
import com.example.prestamos.repository.PrestamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PrestamoService {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private HerramientaRepository herramientaRepository;

    public List<Prestamo> listarPrestamos() {
        return prestamoRepository.findAll();
    }

    public List<Prestamo> listarPrestamosPorHerramienta(Long herramientaId) {
        return prestamoRepository.findByHerramientaId(herramientaId);
    }

    @Transactional
    public Prestamo registrarPrestamo(PrestamoRequest request) {
        Herramienta herramienta = herramientaRepository.findById(request.getHerramientaId())
                .orElseThrow(() -> new RuntimeException("La herramienta no existe."));

        if (request.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad a prestar debe ser mayor a cero.");
        }

        if (request.getCantidad() > herramienta.getCantidad()) {
            throw new IllegalArgumentException("No hay suficientes unidades disponibles.");
        }

        herramienta.setCantidad(herramienta.getCantidad() - request.getCantidad());
        herramientaRepository.save(herramienta);

        Prestamo prestamo = new Prestamo();
        prestamo.setResponsable(request.getResponsable());
        prestamo.setFechaPrestamo(request.getFechaPrestamo());
        prestamo.setCantidad(request.getCantidad());
        prestamo.setHerramienta(herramienta);

        return prestamoRepository.save(prestamo);
    }
}