package com.alimentared.backend.entity;

import com.alimentared.backend.entity.enums.EstadoAlimento;
import com.alimentared.backend.entity.enums.EstadoLote;
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
@Table(name = "lote", indexes = {
        @Index(name = "idx_lote_estado", columnList = "estado"),
        @Index(name = "idx_lote_caducidad", columnList = "fechaCaducidad")
})
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLote;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private TipoAlimento categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donante_id")
    private Organizacion donante;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadInicial;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadDisponible;

    /** RN05: porción mínima que se puede solicitar. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal unidadMinima;

    /** RN10: paso o múltiplo en que se fracciona el lote. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal multiplo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLote estado;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoAlimento estadoAlimento;

    @Column(length = 200)
    private String ubicacion;

    /** RF04/RF06: oculto en catálogo, se revela al asignar. */
    @Column(length = 500)
    private String instruccionesRecogida;

    /** RF04/RF06: oculto en catálogo, se revela al asignar. */
    @Column(length = 150)
    private String contacto;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false)
    private LocalDateTime fechaCaducidad;

    @Version
    private Long version;

    /** RN01: vencido cuando la fecha actual supera la fecha límite. */
    public boolean estaVencido(LocalDateTime ahora) {
        return ahora.isAfter(fechaCaducidad);
    }
}
