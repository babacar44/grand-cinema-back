package fr.grandcinema.config;

import fr.grandcinema.security.JwtAuthenticationFilter;
import fr.grandcinema.security.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * US 4 – Règles d'accès par rôle.
 * API sans session (stateless) : chaque requête porte son jeton JWT.
 */
@Configuration
public class SecurityConfig {

    private static final String JSON_401 = "{\"status\":401,\"detail\":\"Authentification requise\"}";
    private static final String JSON_403 = "{\"status\":403,\"detail\":\"Accès refusé\"}";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                           UserDetailsService userDetailsService) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        http
                .csrf(AbstractHttpConfigurer::disable)                 // pas de cookie de session : CSRF non applicable
                .cors(cors -> { })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/inscription", "/api/auth/connexion").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/films/**", "/api/seances/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/controle/**").hasAnyRole("AGENT", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> ecrire(res, 401, JSON_401))
                        .accessDeniedHandler((req, res, ex) -> ecrire(res, 403, JSON_403)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void ecrire(jakarta.servlet.http.HttpServletResponse res, int status, String json)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json);
    }

    /** Mots de passe chiffrés avec BCrypt (jamais stockés en clair). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** Autorise le front Angular (localhost:4200 en dev, URL Vercel en production). */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origines) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origines.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
