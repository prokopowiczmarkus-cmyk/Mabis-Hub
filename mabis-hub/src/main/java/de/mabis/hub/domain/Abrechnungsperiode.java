package de.mabis.hub.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Abrechnungszeitraum für die Bilanzkreisabrechnung.
 *
 * Rollierende Abrechnung gemäß BK6-24-210-2:
 *   M+1  → Vorläufige Abrechnung  (Prognosen/Schätzwerte für fehlende Messwerte)
 *   M+13 → Korrigierte Abrechnung (nach Jahresablesung, reale Messwerte)
 *   M+67 → Endgültige Abrechnung  (Verjährungsgrenze, keine Änderungen mehr möglich)
 */
public record Abrechnungsperiode(
        YearMonth monat,
        Lauf lauf
) {
    public Abrechnungsperiode {
        Objects.requireNonNull(monat, "monat");
        Objects.requireNonNull(lauf, "lauf");
    }

    public enum Lauf {
        VORLAEUFIG(1, "Vorläufige Abrechnung (M+1)"),
        KORRIGIERT(13, "Korrigierte Abrechnung (M+13)"),
        ENDGUELTIG(67, "Endgültige Abrechnung (M+67)");

        public final int monateNachLiefermonat;
        public final String bezeichnung;

        Lauf(int monateNachLiefermonat, String bezeichnung) {
            this.monateNachLiefermonat = monateNachLiefermonat;
            this.bezeichnung = bezeichnung;
        }
    }

    public LocalDate von() {
        return monat.atDay(1);
    }

    public LocalDate bis() {
        return monat.atEndOfMonth();
    }

    /** Fälligkeitsdatum des Abrechnungslaufs. */
    public YearMonth faelligkeit() {
        return monat.plusMonths(lauf.monateNachLiefermonat);
    }

    @Override
    public String toString() {
        return monat + " – " + lauf.bezeichnung;
    }
}
