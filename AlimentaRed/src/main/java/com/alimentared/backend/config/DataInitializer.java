package com.alimentared.backend.config;

import com.alimentared.backend.entity.TipoAlimento;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.Rol;
import com.alimentared.backend.repository.TipoAlimentoRepository;
import com.alimentared.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Crea los usuarios internos y las categorías base cuando la base de datos está vacía. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "alimentared.seed", name = "habilitado", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final TipoAlimentoRepository tipoAlimentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AlimentaRedProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public void run(String... args) {
        AlimentaRedProperties.Seed seed = properties.seed();
        crearUsuarioSiNoExiste(Rol.ADMINISTRADOR, "Administrador AlimentaRed",
                seed.adminCorreo(), seed.adminContrasena());
        crearUsuarioSiNoExiste(Rol.COORDINADOR, "Coordinador Red Comunitaria",
                seed.coordinadorCorreo(), seed.coordinadorContrasena());
        crearCategoriasBase();
    }

    private void crearUsuarioSiNoExiste(Rol rol, String nombre, String correo, String contrasena) {
        if (usuarioRepository.existsByRol(rol) || usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .nombre(nombre)
                .correo(correo.toLowerCase())
                .contrasena(passwordEncoder.encode(contrasena))
                .rol(rol)
                .fechaRegistro(LocalDateTime.now(clock))
                .build());
        log.info("Usuario {} inicial creado: {}", rol, correo);
    }

    private void crearCategoriasBase() {
        if (tipoAlimentoRepository.count() > 0) {
            return;
        }
        tipoAlimentoRepository.saveAll(List.of(
                categoria("Frutas y verduras", "Productos frescos de origen vegetal", "KG"),
                categoria("Panadería", "Pan, pasteles y productos horneados", "UNIDAD"),
                categoria("Lácteos", "Leche, yogures y derivados", "L"),
                categoria("Proteínas", "Carnes, pollo, pescado y huevos", "KG"),
                categoria("Comidas preparadas", "Platos listos para consumo inmediato", "PORCION"),
                categoria("Granos y abarrotes", "Arroz, granos, enlatados y no perecederos", "KG")
        ));
        log.info("Categorías de alimentos base creadas");
    }

    private TipoAlimento categoria(String nombre, String descripcion, String unidad) {
        return TipoAlimento.builder().nombre(nombre).descripcion(descripcion).unidadMedida(unidad).build();
    }
}
