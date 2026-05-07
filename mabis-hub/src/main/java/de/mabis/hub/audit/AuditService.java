package de.mabis.hub.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 * Schreibt Audit-Einträge in die Datenbank.
 *
 * Technische Entscheidungen:
 * - @Async: Audit-Schreiben blockiert den Request-Thread nicht
 * - Propagation.REQUIRES_NEW: Audit-Log wird auch bei Rollback der Haupttransaktion gespeichert
 *   (fehlgeschlagene Operationen müssen protokolliert sein)
 * - Keine eigene Exception-Propagation: Audit-Fehler dürfen die Geschäftsoperation nicht abbrechen
 */
@Service
public class AuditService {

    private static final Logger LOG = Logger.getLogger(AuditService.class.getName());

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void protokollieren(AuditEintrag eintrag) {
        try {
            repository.save(AuditEntity.vonDomain(eintrag));
        } catch (Exception ex) {
            // Audit-Fehler nie an den Aufrufer propagieren
            LOG.severe("Audit-Log konnte nicht geschrieben werden: " + ex.getMessage()
                    + " | Ereignis: " + eintrag.ereignis());
        }
    }

    // ── Abfrageoperationen (ADMIN) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AuditEintrag> alleEintraege(int seite, int groesse) {
        return repository.findByOrderByZeitpunktDesc(PageRequest.of(seite, groesse))
                .map(AuditEntity::zuDomain);
    }

    @Transactional(readOnly = true)
    public Page<AuditEintrag> nachPrincipal(String principalId, int seite, int groesse) {
        return repository.findByPrincipalIdOrderByZeitpunktDesc(
                principalId, PageRequest.of(seite, groesse)).map(AuditEntity::zuDomain);
    }

    @Transactional(readOnly = true)
    public Page<AuditEintrag> nachMaloId(String maloId, int seite, int groesse) {
        return repository.findByMaloIdOrderByZeitpunktDesc(
                maloId, PageRequest.of(seite, groesse)).map(AuditEntity::zuDomain);
    }

    @Transactional(readOnly = true)
    public Page<AuditEintrag> nachBilanzkreis(String bilanzkreisId, int seite, int groesse) {
        return repository.findByBilanzkreisIdOrderByZeitpunktDesc(
                bilanzkreisId, PageRequest.of(seite, groesse)).map(AuditEntity::zuDomain);
    }

    @Transactional(readOnly = true)
    public Page<AuditEintrag> imZeitraum(Instant von, Instant bis, int seite, int groesse) {
        return repository.findImZeitraum(von, bis, PageRequest.of(seite, groesse))
                .map(AuditEntity::zuDomain);
    }

    @Transactional(readOnly = true)
    public List<AuditEintrag> letzteFehlschlaege(int anzahl) {
        return repository.findFehlerEintraege(PageRequest.of(0, anzahl))
                .stream().map(AuditEntity::zuDomain).toList();
    }

    @Transactional(readOnly = true)
    public AuditStatistik statistik() {
        long gesamt     = repository.count();
        long fehlschlag = repository.countByErfolg(false);
        return new AuditStatistik(gesamt, gesamt - fehlschlag, fehlschlag);
    }

    public record AuditStatistik(long gesamt, long erfolgreich, long fehlgeschlagen) {}
}
