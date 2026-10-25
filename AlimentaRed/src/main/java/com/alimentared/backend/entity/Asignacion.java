package com.alimentared.backend.entity;

import com.alimentared.backend.entity.enums.EstadoAsignacion;
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
@Table(name = "asignacion")
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAsignacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", unique = true)
    private Solicitud solicitud;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadAsignada;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadRetirada = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoAsignacion estado;

    @Builder.Default
    @Column(nullable = false)
    private boolean autoAsignada = false;

    @Column(nullable = false)
    private LocalDateTime fechaAsignacion;

    /** Franja horaria de retiro: fuera de ella se genera strike (RF07). */
    @Column(nullable = false)
    private LocalDateTime fechaLimiteRetiro;

    private LocalDateTime fechaCierre;

    /** Null cuando la asignó el sistema (auto-asignación FCFS). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsable_id")
    private Usuario usuarioResponsable;

    @Version
    private Long version;

    public BigDecimal getCantidadPendiente() {
        return cantidadAsignada.subtract(cantidadRetirada);
    }
}
