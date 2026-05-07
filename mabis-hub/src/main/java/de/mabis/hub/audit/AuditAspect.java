package de.mabis.hub.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * AOP-Aspekt: Fängt alle @Auditiert-annotierten Controller-Methoden ab.
 *
 * Für jeden Aufruf wird protokolliert:
 *  - Wer  (JWT subject + Marktrolle)
 *  - Was  (AuditEreignis)
 *  - Womit (MaLo-ID, Bilanzkreis-ID aus dem Request-Body)
 *  - Wann (Timestamp, automatisch durch AuditService)
 *  - Von wo (Client-IP)
 *  - Ergebnis (Erfolg / Fehlermeldung)
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(auditiert)")
    public Object protokollieren(ProceedingJoinPoint joinPoint, Auditiert auditiert) throws Throwable {
        AuditEintrag.Builder builder = AuditEintrag.builder(auditiert.ereignis())
                .principalId(extractPrincipalId())
                .marktRolle(extractMarktRolle())
                .anfrageIp(extractClientIp());

        // Kontextdaten aus Request-Argumenten extrahieren
        Object[] args = joinPoint.getArgs();
        if (!auditiert.maloIdFeld().isEmpty()) {
            extractFeld(args, auditiert.maloIdFeld()).ifPresent(builder::maloId);
        }
        if (!auditiert.bilanzkreisIdFeld().isEmpty()) {
            extractFeld(args, auditiert.bilanzkreisIdFeld()).ifPresent(builder::bilanzkreisId);
        }

        // Methodenname als Detail
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        builder.details("{\"methode\":\"" + method.getName() + "\",\"argAnzahl\":" + args.length + "}");

        try {
            Object ergebnis = joinPoint.proceed();
            auditService.protokollieren(builder.erfolg(true).build());
            return ergebnis;
        } catch (Exception ex) {
            auditService.protokollieren(builder.fehlerMeldung(kuerzen(ex.getMessage(), 500)).build());
            throw ex;
        }
    }

    private String extractPrincipalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "anonym";
        if (auth.getPrincipal() instanceof Jwt jwt) return jwt.getSubject();
        return auth.getName();
    }

    private String extractMarktRolle() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .filter(r -> !r.isEmpty())
                .findFirst()
                .orElse(null);
    }

    private String extractClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            return (forwarded != null) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sucht in allen Request-Argumenten per Reflection nach einem Feld mit dem gegebenen Namen.
     * Funktioniert mit DTOs (Records und Classes).
     */
    private Optional<String> extractFeld(Object[] args, String feldName) {
        return Arrays.stream(args)
                .filter(arg -> arg != null)
                .flatMap(arg -> Arrays.stream(arg.getClass().getDeclaredFields())
                        .filter(f -> f.getName().equals(feldName))
                        .map(f -> {
                            try {
                                f.setAccessible(true);
                                Object val = f.get(arg);
                                return val != null ? val.toString() : null;
                            } catch (IllegalAccessException e) {
                                return null;
                            }
                        }))
                .filter(v -> v != null)
                .findFirst();
    }

    private String kuerzen(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
