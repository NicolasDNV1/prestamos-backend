package com.alimentared.backend.entity;

import com.alimentared.backend.entity.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Solo Donante y Receptora pertenecen a una organización. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacion_id", unique = true)
    private Organizacion organizacion;

    /** Hash BCrypt, nunca texto plano. */
    @Column(nullable = false)
    private String contrasena;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
