package de.mabis.hub.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Ergebnis eines Bilanzkreisabrechnungslaufs.
 * Wird vom MaBiS-Hub erzeugt und dem BKV zugestellt.
 * Ersetzt die bisherige dezentrale ÜNB-Abrechnung.
 */
public record Abrechnung(
        String abrechnungsId,
        String bilanzkreisId,
        String bkvId,
        Abrechnungsperiode periode,
        double istVerbrauchKwh,
        double fahrplanKwh,
        AusgleichsEnergieSaldo ausgleichsEnergieSaldo,
        double nettoKostenEur,
        AbrechnungsStatus status,
        Instant erstelltAm
) {
    public Abrechnung {
        Objects.requireNonNull(abrechnungsId, "abrechnungsId");
        Objects.requireNonNull(bilanzkreisId, "bilanzkreisId");
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(ausgleichsEnergieSaldo, "ausgleichsEnergieSaldo");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(erstelltAm, "erstelltAm");
    }

    public enum AbrechnungsStatus {
        ERSTELLT,       // Abrechnung berechnet, noch nicht zugestellt
        ZUGESTELLT,     // An BKV übermittelt
        ANERKANNT,      // BKV hat Abrechnung bestätigt
        WIDERSPROCHEN,  // BKV hat Widerspruch eingelegt
        ABGESCHLOSSEN   // Endgültig abgerechnet (M+67)
    }

    /** Abweichung Ist vs. Fahrplan in kWh. */
    public double abweichungKwh() {
        return istVerbrauchKwh - fahrplanKwh;
    }

    /** Abweichung in Prozent relativ zum Fahrplan. */
    public double abweichungProzent() {
        if (fahrplanKwh == 0) return 0;
        return (abweichungKwh() / fahrplanKwh) * 100.0;
    }

    public boolean istEndgueltig() {
        return periode.lauf() == Abrechnungsperiode.Lauf.ENDGUELTIG;
    }

    @Override
    public String toString() {
        return String.format(
            "Abrechnung[%s | %s | Ist=%.1f kWh | Fahrplan=%.1f kWh | Abw=%.1f%% | AE-Kosten=%.2f EUR | %s]",
            abrechnungsId, periode, istVerbrauchKwh, fahrplanKwh,
            abweichungProzent(), ausgleichsEnergieSaldo.ausgleichsenergiekostenEur(), status);
    }
}
