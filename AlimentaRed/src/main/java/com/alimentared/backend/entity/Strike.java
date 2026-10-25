package com.alimentared.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "strike")
public class Strike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idStrike;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacion_id")
    private Organizacion organizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id")
    private Asignacion asignacion;

    @Column(nullable = false, length = 120)
    private String nombreStrike;

    @Column(length = 500)
    private String detalles;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    /** RN12: un strike anulado por apelación deja de contar. */
    @Builder.Default
    @Column(nullable = false)
    private boolean vigente = true;

    @Column(length = 500)
    private String justificacionApelacion;

    private LocalDateTime fechaApelacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_evaluador_id")
    private Usuario usuarioEvaluador;
}
