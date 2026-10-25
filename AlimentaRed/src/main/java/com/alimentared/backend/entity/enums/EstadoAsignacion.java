package com.alimentared.backend.entity.enums;

import java.util.List;

public enum EstadoAsignacion {
    ACTIVA,
    RETIRO_PARCIAL,
    COMPLETADA,
    NO_RECOGIDA,
    CANCELADA,
    CERRADA_CON_RESTOCK;

    public static final List<EstadoAsignacion> ABIERTOS = List.of(ACTIVA, RETIRO_PARCIAL);
}
