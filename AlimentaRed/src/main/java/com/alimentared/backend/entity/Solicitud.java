package com.alimentared.backend.entity;

import com.alimentared.backend.entity.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "solicitud", indexes = {
        @Index(name = "idx_solicitud_estado_fecha", columnList = "estado, fechaCreacion")
})
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacion_id")
    private Organizacion organizacion;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadSolicitada;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaResolucion;

    /** RN09: varias solicitudes en la misma ventana; decide el donante. */
    @Builder.Default
    @Column(nullable = false)
    private boolean enEmpate = false;

    @Column(length = 500)
    private String motivoResolucion;

    @Version
    private Long version;
}
