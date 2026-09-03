package com.integrador.Turismo.Config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.integrador.Turismo.Monitoring.filter.RequestLoggingFilter;
import com.integrador.Turismo.Security.JwtFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize en controllers
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final RequestLoggingFilter requestLoggingFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Actuator (Monitoreo) ───────────────────────────
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/actuator/metrics").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/api/test").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // ── Públicas ──────────────────────────────────────────
                        .requestMatchers("/api/auth/**").permitAll() // login, register
                        .requestMatchers("/api/paquetes/**").permitAll() // ver tours sin login
                        .requestMatchers("/api/lugares/**").permitAll() // ver lugares
                        .requestMatchers("/api/usuarios/perfil").authenticated()
                        .requestMatchers("/api/tickets/**").authenticated()

                        // ── Solo ADMIN ────────────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // ── Cualquier autenticado ─────────────────────────────
                        .anyRequest().authenticated())
                .authenticationProvider(authProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS: permite peticiones desde tu frontend en Next.js
    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000", // Next.js dev
                "https://proyecto-integrador-chi-ten.vercel.app" // Cambia por tu URL de Vercel
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
