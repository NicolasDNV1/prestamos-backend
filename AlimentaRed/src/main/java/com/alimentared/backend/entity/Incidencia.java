package com.alimentared.backend.entity;

import com.alimentared.backend.entity.enums.EstadoLote;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidencia")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idIncidencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(nullable = false, length = 500)
    private String detalles;

    /** Estado del lote antes de bloquearse, para poder reactivarlo. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLote estadoLoteAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_reporta_id")
    private Usuario usuarioReporta;

    @Column(nullable = false)
    private LocalDateTime fechaReporte;

    @Builder.Default
    @Column(nullable = false)
    private boolean resuelta = false;

    @Column(length = 500)
    private String resolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_resuelve_id")
    private Usuario usuarioResuelve;

    private LocalDateTime fechaResolucion;
}
