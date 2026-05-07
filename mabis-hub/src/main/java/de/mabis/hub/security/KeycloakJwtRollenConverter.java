package de.mabis.hub.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Extrahiert Marktrollen aus Keycloak-JWTs.
 *
 * Keycloak legt Rollen in zwei Claims ab:
 *   realm_access.roles[]          → mandantenübergreifende Rollen
 *   resource_access.<client>.roles[] → anwendungsspezifische Rollen
 *
 * Beide werden zu Spring-Security-Authorities mit Präfix "ROLE_" gemappt.
 */
public class KeycloakJwtRollenConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String clientId;

    public KeycloakJwtRollenConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return Stream.concat(realmRollen(jwt), clientRollen(jwt))
                .distinct()
                .map(rolle -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rolle))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Stream<String> realmRollen(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return Stream.empty();
        List<String> rollen = (List<String>) realmAccess.getOrDefault("roles", Collections.emptyList());
        return rollen.stream();
    }

    @SuppressWarnings("unchecked")
    private Stream<String> clientRollen(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || !resourceAccess.containsKey(clientId)) return Stream.empty();
        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        List<String> rollen = (List<String>) clientAccess.getOrDefault("roles", Collections.emptyList());
        return rollen.stream();
    }
}
