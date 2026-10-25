package com.alimentared.backend.entity;

import com.alimentared.backend.entity.enums.EstadoEntrega;
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
@Table(name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEntrega estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id")
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_id")
    private Asignacion asignacion;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadRetirada;

    @Column(nullable = false)
    private LocalDateTime fechaEntrega;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_responsable_id")
    private Usuario usuarioResponsable;

    @Builder.Default
    @Column(nullable = false)
    private boolean fueraDeFranja = false;
}
