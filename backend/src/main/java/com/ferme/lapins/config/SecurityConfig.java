package com.ferme.lapins.config;

import com.ferme.lapins.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    @Value("${app.cors.origins}") private String origins;

    /**
     * Rôles : PROPRIETAIRE (tout), OUVRIER (tout sauf l'argent, les clients et l'équipe), LECTEUR (consultation seule).
     * L'ordre des règles compte : la première qui correspond s'applique.
     */
    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**", "/actuator/health", "/error",
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api/export/telecharger").permitAll()
                .requestMatchers("/api/compte/**", "/api/appareils/**", "/api/assistant/**", "/api/previsions").authenticated()
                .requestMatchers("/api/equipe/**").hasRole("PROPRIETAIRE")
                .requestMatchers("/api/mouvements/**", "/api/bilan", "/api/statistiques",
                        "/api/clients/**", "/api/commandes/**", "/api/lots/*/vente", "/api/export/**").hasRole("PROPRIETAIRE")
                .requestMatchers(HttpMethod.PUT, "/api/config").hasRole("PROPRIETAIRE")
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers("/api/**").hasAnyRole("PROPRIETAIRE", "OUVRIER")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401, "Non authentifié")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(origins.split(",")));
        c.setAllowedMethods(List.of("*"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
