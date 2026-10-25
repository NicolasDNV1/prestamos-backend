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
@Table(name = "organizacion")
public class Organizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOrganizacion;

    @Column(nullable = false, unique = true, length = 20)
    private String nit;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 200)
    private String ubicacion;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(length = 30)
    private String telefono;

    /** RF02: una organización nueva queda pendiente de verificación. */
    @Builder.Default
    @Column(nullable = false)
    private boolean verificada = false;

    /** RN08: cuenta suspendida al acumular el máximo de strikes. */
    @Builder.Default
    @Column(nullable = false)
    private boolean bloqueada = false;

    @Builder.Default
    @Column(nullable = false)
    private int strikesVigentes = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Version
    private Long version;
}
