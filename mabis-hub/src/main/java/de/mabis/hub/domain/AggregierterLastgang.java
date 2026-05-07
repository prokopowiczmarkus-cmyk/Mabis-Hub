package de.mabis.hub.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Aggregierter Lastgang für einen Bilanzkreis über einen Abrechnungszeitraum.
 * Datenschutz: Enthält nur aggregierte Werte – keine Einzelmesswerte einzelner Kunden.
 * Mindestclustergröße: 5 Marktlokationen (Pflicht ab 2030 für Privatkunden).
 */
public record AggregierterLastgang(
        String bilanzkreisId,
        LocalDate von,
        LocalDate bis,
        List<Double> viertelstundenwerte,
        int anzahlMalos,
        String netzgebiet
) {
    public static final int MIN_CLUSTER_GROESSE = 5;

    public AggregierterLastgang {
        Objects.requireNonNull(bilanzkreisId, "bilanzkreisId");
        Objects.requireNonNull(von, "von");
        Objects.requireNonNull(bis, "bis");
        Objects.requireNonNull(viertelstundenwerte, "viertelstundenwerte");
        if (anzahlMalos < 1) throw new IllegalArgumentException("Mindestens 1 MaLo erforderlich");
    }

    public boolean erfuelltDatenschutzanforderung() {
        return anzahlMalos >= MIN_CLUSTER_GROESSE;
    }

    public double gesamtverbrauchKwh() {
        return viertelstundenwerte.stream().mapToDouble(Double::doubleValue).sum();
    }

    public double spitzenlastKw() {
        return viertelstundenwerte.stream().mapToDouble(Double::doubleValue).max().orElse(0.0) * 4;
    }
}
