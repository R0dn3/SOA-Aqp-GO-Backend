package com.integrador.Turismo.Config;

import com.integrador.Turismo.Model.Lugar;
import com.integrador.Turismo.Model.Proveedor;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Repository.LugarRepository;
import com.integrador.Turismo.Repository.ProveedorRepository;
import com.integrador.Turismo.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final LugarRepository   lugarRepository;
    private final PasswordEncoder   passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            crearAdmin();
            crearLugares();
            crearProveedores();
        };
    }

    // ── Admin ─────────────────────────────────────────────────

    private void crearAdmin() {
        String adminEmail = "admin@aqpgo.com";
        if (usuarioRepository.existsByEmail(adminEmail)) {
            log.info("✅ Admin ya existe.");
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .nombreCompleto("Administrador AqpGo")
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin1234!"))
                .rol(Usuario.Rol.ADMIN)
                .build());
        log.info("🚀 Admin creado → email: admin@aqpgo.com / pass: Admin1234!");
    }

    // ── Lugares de prueba ─────────────────────────────────────

    private void crearLugares() {
        if (lugarRepository.count() > 0) {
            log.info("✅ Lugares ya existen.");
            return;
        }

        List<Lugar> lugares = List.of(
                Lugar.builder()
                        .nombre("Arequipa")
                        .descripcion("La Ciudad Blanca, famosa por su arquitectura de sillar.")
                        .region("Arequipa")
                        .build(),
                Lugar.builder()
                        .nombre("Cañón del Colca")
                        .descripcion("Uno de los cañones más profundos del mundo.")
                        .region("Arequipa")
                        .build(),
                Lugar.builder()
                        .nombre("Oasis de Sangalle")
                        .descripcion("Paraíso natural en el fondo del Cañón del Colca.")
                        .region("Arequipa")
                        .build(),
                Lugar.builder()
                        .nombre("Volcán Misti")
                        .descripcion("Icónico volcán que domina el paisaje de Arequipa.")
                        .region("Arequipa")
                        .build(),
                Lugar.builder()
                        .nombre("Valle del Colca")
                        .descripcion("Valle fértil con pueblos coloniales y terrazas andinas.")
                        .region("Arequipa")
                        .build(),
                Lugar.builder()
                        .nombre("Campiña Arequipeña")
                        .descripcion("Zona rural de chacras, molinos y paisajes verdes.")
                        .region("Arequipa")
                        .build()
        );

        lugarRepository.saveAll(lugares);
        log.info("🗺️  {} lugares de prueba creados.", lugares.size());
    }
    void crearProveedores() {

        if (proveedorRepository.count() > 0) {
            log.info("✅ Proveedores ya existen.");
            return;
        }

        List<Proveedor> proveedores = List.of(

                Proveedor.builder()
                        .nombre("Turismo Colca Express")
                        .tipo(Proveedor.Tipo.TRANSPORTE)
                        .telefono("987654321")
                        .email("contacto@colcaexpress.com")
                        .notas("Servicio de transporte turístico al Valle del Colca.")
                        .build(),

                Proveedor.builder()
                        .nombre("Hotel Misti Plaza")
                        .tipo(Proveedor.Tipo.HOTEL)
                        .telefono("954321678")
                        .email("reservas@mistiplaza.com")
                        .notas("Hotel céntrico con desayuno incluido.")
                        .build(),

                Proveedor.builder()
                        .nombre("Picantería La Tradición")
                        .tipo(Proveedor.Tipo.RESTAURANTE)
                        .telefono("912345678")
                        .email("latradicion@restaurant.com")
                        .notas("Especialidad en comida típica arequipeña.")
                        .build()
        );

        proveedorRepository.saveAll(proveedores);

        log.info("🏨 {} proveedores de prueba creados.", proveedores.size());
    }
}
