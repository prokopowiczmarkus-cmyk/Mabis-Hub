package de.mabis.hub.api;

import de.mabis.hub.domain.*;
import de.mabis.hub.service.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Öffentliche API des MaBiS-Hubs gegenüber den Marktteilnehmern.
 *
 * Schnittstellen nach Marktrolle:
 *   - MSB  → messwertEinliefern()
 *   - VNB  → stammdatenMelden()
 *   - BKV  → fahrplanEinreichen(), abrechnungAbrufen()
 *   - ÜNB  → abrechnungslaufAnstoßen() [Hub-interner Betrieb]
 *
 * Datenprinzip: Jeder Aufruf referenziert MaLo-IDs, nie Personendaten.
 */
@Service
public class MaBiSHubFassade {

    private final StammdatenService stammdatenService;
    private final MesswertVerarbeitungService messwertService;
    private final AggregationsService aggregationsService;
    private final FahrplanService fahrplanService;
    private final BilanzkreisAbrechnungService abrechnungService;

    public MaBiSHubFassade(StammdatenService stammdatenService,
                            MesswertVerarbeitungService messwertService,
                            AggregationsService aggregationsService,
                            FahrplanService fahrplanService,
                            BilanzkreisAbrechnungService abrechnungService) {
        this.stammdatenService   = stammdatenService;
        this.messwertService     = messwertService;
        this.aggregationsService = aggregationsService;
        this.fahrplanService     = fahrplanService;
        this.abrechnungService   = abrechnungService;
    }

    // ── MSB-Schnittstelle ────────────────────────────────────────────────────

    /** Messstellenbetreiber liefert einen Messwert ein. */
    public void messwertEinliefern(Messwert messwert) {
        messwertService.messwertEmpfangen(messwert);
    }

    // ── VNB-Schnittstelle ────────────────────────────────────────────────────

    /** Verteilnetzbetreiber meldet Stammdaten einer Marktlokation. */
    public void stammdatenMelden(Stammdaten stammdaten) {
        stammdatenService.stammdatenEmpfangen(stammdaten);
    }

    // ── BKV-Schnittstelle ────────────────────────────────────────────────────

    /** Bilanzkreisverantwortlicher reicht Tagesfahrplan ein. */
    public void fahrplanEinreichen(Fahrplan fahrplan) {
        fahrplanService.fahrplanEinreichen(fahrplan);
    }

    /** BKV ruft seine Abrechnung nach Abrechnungs-ID ab. */
    public Abrechnung abrechnungAbrufen(String abrechnungsId) {
        return abrechnungService.abrechnungLaden(abrechnungsId)
                .orElseThrow(() -> new NoSuchElementException("Abrechnung nicht gefunden: " + abrechnungsId));
    }

    /** Alle Abrechnungen eines Bilanzkreises (alle Läufe). */
    public List<Abrechnung> alleAbrechnungen(String bilanzkreisId) {
        return abrechnungService.alleAbrechnungen(bilanzkreisId);
    }

    // ── ÜNB / Hub-intern ─────────────────────────────────────────────────────

    /**
     * Startet einen Abrechnungslauf (vorläufig / korrigiert / endgültig).
     * @param aePreisEurKwh  Aktueller Ausgleichsenergiepreis in EUR/kWh
     */
    public Abrechnung abrechnungslaufAnstoßen(String bilanzkreisId,
                                               YearMonth yearMonth,
                                               Abrechnungsperiode.Lauf lauf,
                                               String netzgebiet,
                                               double aePreisEurKwh) {
        return abrechnungService.abrechnungslaufDurchfuehren(
                bilanzkreisId, yearMonth, lauf, netzgebiet, aePreisEurKwh);
    }

    /** Gibt MaLo-IDs mit fehlenden Messwerten zurück (Klärfallmanagement). */
    public List<MaLoId> klaerfaelleAbrufen(String bilanzkreisId, LocalDate stichtag) {
        java.time.Instant von = stichtag.atStartOfDay(java.time.ZoneId.of("Europe/Berlin")).toInstant();
        java.time.Instant bis = stichtag.plusDays(1).atStartOfDay(java.time.ZoneId.of("Europe/Berlin")).toInstant();
        return aggregationsService.fehlendeMalos(bilanzkreisId, stichtag, von, bis);
    }

    // ── Monitoring ───────────────────────────────────────────────────────────

    public HubStatus status() {
        return new HubStatus(
                stammdatenService.gesamtanzahlMalos(),
                (int) messwertService.anzahlMesswerteJeMalo().getOrDefault("gesamt", 0L)
        );
    }

    public record HubStatus(int registrierteMalos, int gespeicherteMesswerte) {}

    private static class NoSuchElementException extends RuntimeException {
        NoSuchElementException(String msg) { super(msg); }
    }
}
