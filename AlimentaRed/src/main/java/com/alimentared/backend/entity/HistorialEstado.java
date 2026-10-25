package com.alimentared.backend.entity;

import com.alimentared.backend.entity.enums.EntidadAuditada;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** RN04: traza de cada cambio de estado con fecha y usuario responsable. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historial_estado", indexes = {
        @Index(name = "idx_historial_entidad", columnList = "entidad, entidadId")
})
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistorial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntidadAuditada entidad;

    @Column(nullable = false)
    private Long entidadId;

    @Column(length = 30)
    private String estadoAnterior;

    @Column(nullable = false, length = 30)
    private String estadoNuevo;

    /** Null cuando el cambio lo ejecuta el sistema (tareas programadas). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsable_id")
    private Usuario usuarioResponsable;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 500)
    private String justificacion;
}
