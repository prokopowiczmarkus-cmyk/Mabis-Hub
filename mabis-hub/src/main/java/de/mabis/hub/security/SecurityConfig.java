package de.mabis.hub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sicherheitskonfiguration des MaBiS-Hubs.
 *
 * Architekturprinzip: Jede Marktrolle erhält ausschließlich Zugriff auf die
 * Endpunkte, die ihrer gesetzlichen Funktion entsprechen.
 *
 *  MSB  → POST  /api/v1/messwerte
 *  VNB  → POST  /api/v1/stammdaten
 *  BKV  → POST  /api/v1/fahrplaene
 *         GET   /api/v1/abrechnungen/**
 *  UNB  → POST  /api/v1/abrechnungen/laeufe  (Hub-Betrieb)
 *  ADMIN→ alle Endpunkte (Lesezugriff Regulierungsbehörde)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${mabis.security.keycloak.client-id:mabis-hub}")
    private String keycloakClientId;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // Swagger / OpenAPI – öffentlich
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**"
                ).permitAll()

                // Aktuatorendpunkte – öffentlich (kein Actuator im Scope, Platzhalter)
                .requestMatchers("/actuator/health").permitAll()

                // MSB: Messwerteinlieferung
                .requestMatchers(HttpMethod.POST, "/api/v1/messwerte")
                    .hasAnyRole("MSB", "ADMIN")

                // VNB: Stammdatenmeldung
                .requestMatchers(HttpMethod.POST, "/api/v1/stammdaten")
                    .hasAnyRole("VNB", "ADMIN")

                // BKV: Fahrplaneinreichung
                .requestMatchers(HttpMethod.POST, "/api/v1/fahrplaene")
                    .hasAnyRole("BKV", "ADMIN")

                // ÜNB: Abrechnungslauf anstoßen (Hub-intern)
                .requestMatchers(HttpMethod.POST, "/api/v1/abrechnungen/laeufe")
                    .hasAnyRole("UNB", "ADMIN")

                // BKV + ÜNB + Admin: Abrechnungen abrufen
                .requestMatchers(HttpMethod.GET, "/api/v1/abrechnungen/**")
                    .hasAnyRole("BKV", "UNB", "ADMIN")

                // Hub-Status: alle authentifizierten Marktteilnehmer
                .requestMatchers(HttpMethod.GET, "/api/v1/abrechnungen/status")
                    .authenticated()

                // Admin: Audit-Log (alle Methoden)
                .requestMatchers("/api/v1/audit/**")
                    .hasRole("ADMIN")

                // Alles andere erfordert Authentifizierung
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
            new KeycloakJwtRollenConverter(keycloakClientId)
        );
        return converter;
    }
}
