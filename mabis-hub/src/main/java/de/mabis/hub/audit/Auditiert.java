package de.mabis.hub.audit;

import java.lang.annotation.*;

/**
 * Markiert eine Controller-Methode als audit-pflichtig.
 * Der AuditAspect fängt annotierte Methoden ab und schreibt
 * automatisch einen Eintrag in das Audit-Log.
 *
 * Verwendung:
 * <pre>
 *   @PostMapping
 *   @Auditiert(ereignis = AuditEreignis.STAMMDATEN_GEMELDET, maloIdFeld = "maloId")
 *   public void stammdatenMelden(@RequestBody StammdatenRequest req) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditiert {

    /** Welches Geschäftsereignis wird protokolliert. */
    AuditEreignis ereignis();

    /**
     * Name des Felds im Request-Body, das die MaLo-ID enthält.
     * Leer lassen, wenn kein MaLo-Bezug vorhanden.
     */
    String maloIdFeld() default "";

    /**
     * Name des Felds im Request-Body, das die Bilanzkreis-ID enthält.
     * Leer lassen, wenn kein Bilanzkreis-Bezug vorhanden.
     */
    String bilanzkreisIdFeld() default "";
}
