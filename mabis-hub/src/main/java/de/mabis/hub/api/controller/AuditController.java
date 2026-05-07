package de.mabis.hub.api.controller;

import de.mabis.hub.audit.AuditEintrag;
import de.mabis.hub.audit.AuditService;
import de.mabis.hub.audit.AuditService.AuditStatistik;
import de.mabis.hub.security.MarktRolle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * ADMIN-only: Abfrage des Audit-Logs.
 * Zugang ausschließlich für Regulierungsbehörde / Hub-Admin.
 * Datenhaltungspflicht §257 HGB: 10 Jahre.
 */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit-Log (ADMIN)", description = "Lückenlose Nachvollziehbarkeit aller Hub-Operationen")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Alle Einträge (paginiert)",
               description = "Gibt alle Audit-Einträge absteigend nach Zeitstempel zurück.")
    public Page<AuditEintrag> alleEintraege(
            @RequestParam(defaultValue = "0")  int seite,
            @RequestParam(defaultValue = "50") int groesse) {
        return auditService.alleEintraege(seite, groesse);
    }

    @GetMapping("/principal/{principalId}")
    @Operation(summary = "Audit-Log nach Nutzer",
               description = "Alle Operationen eines bestimmten JWT-Principals.")
    public Page<AuditEintrag> nachPrincipal(
            @Parameter(description = "JWT subject (Nutzer-ID im IdP)")
            @PathVariable String principalId,
            @RequestParam(defaultValue = "0")  int seite,
            @RequestParam(defaultValue = "50") int groesse) {
        return auditService.nachPrincipal(principalId, seite, groesse);
    }

    @GetMapping("/malo/{maloId}")
    @Operation(summary = "Audit-Log nach MaLo-ID",
               description = "Alle Operationen, die eine bestimmte Marktlokation betreffen.")
    public Page<AuditEintrag> nachMaloId(
            @PathVariable String maloId,
            @RequestParam(defaultValue = "0")  int seite,
            @RequestParam(defaultValue = "50") int groesse) {
        return auditService.nachMaloId(maloId, seite, groesse);
    }

    @GetMapping("/bilanzkreis/{bilanzkreisId}")
    @Operation(summary = "Audit-Log nach Bilanzkreis")
    public Page<AuditEintrag> nachBilanzkreis(
            @PathVariable String bilanzkreisId,
            @RequestParam(defaultValue = "0")  int seite,
            @RequestParam(defaultValue = "50") int groesse) {
        return auditService.nachBilanzkreis(bilanzkreisId, seite, groesse);
    }

    @GetMapping("/zeitraum")
    @Operation(summary = "Audit-Log im Zeitraum",
               description = "Alle Operationen zwischen zwei ISO-8601-Zeitstempeln.")
    public Page<AuditEintrag> imZeitraum(
            @Parameter(description = "Von (ISO-8601, z.B. 2026-01-01T00:00:00Z)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant von,
            @Parameter(description = "Bis (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant bis,
            @RequestParam(defaultValue = "0")  int seite,
            @RequestParam(defaultValue = "50") int groesse) {
        return auditService.imZeitraum(von, bis, seite, groesse);
    }

    @GetMapping("/fehler")
    @Operation(summary = "Letzte Fehlschläge",
               description = "Gibt die letzten fehlgeschlagenen Operationen zurück.")
    public List<AuditEintrag> letzteFehlschlaege(
            @RequestParam(defaultValue = "20") int anzahl) {
        return auditService.letzteFehlschlaege(anzahl);
    }

    @GetMapping("/statistik")
    @Operation(summary = "Audit-Statistik",
               description = "Gesamtanzahl Einträge, Erfolgsquote.")
    public AuditStatistik statistik() {
        return auditService.statistik();
    }
}
