package com.alimentared.backend.entity.enums;

import java.util.List;

public enum EstadoSolicitud {
    APROBADO,
    CANCELADO,
    PENDIENTE,
    RECHAZADA,
    EXPIRADA,
    COMPLETADA;

    /** RN07: estados que cuentan como solicitud activa (Pendiente o Asignada). */
    public static final List<EstadoSolicitud> ACTIVOS = List.of(PENDIENTE, APROBADO);
}
