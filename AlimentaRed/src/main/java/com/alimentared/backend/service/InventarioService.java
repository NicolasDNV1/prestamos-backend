package com.alimentared.backend.service;

import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EstadoLote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Operaciones internas de inventario y ciclo de vida del lote (RN01, RN02, RN03, RN05, RN10, RN11). */
public interface InventarioService {

    /** Bloqueo pesimista del lote con su estado más reciente para operaciones transaccionales. */
    Lote bloquearLote(Long idLote);

    void validarLoteSolicitable(Lote lote, LocalDateTime ahora);

    void validarFraccionamiento(Lote lote, BigDecimal cantidad);

    void descontar(Lote lote, BigDecimal cantidad, Usuario responsable);

    void reponer(Lote lote, BigDecimal cantidad, Usuario responsable, String justificacion);

    void evaluarCierreLote(Lote lote, Usuario responsable);

    void cambiarEstado(Lote lote, EstadoLote nuevoEstado, Usuario responsable, String justificacion);
}
