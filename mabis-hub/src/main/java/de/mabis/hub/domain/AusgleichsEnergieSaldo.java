package de.mabis.hub.domain;

import java.util.List;
import java.util.Objects;

/**
 * Viertelstundenscharfer Saldo aus Ist-Werten minus Fahrplan.
 *
 * Positiver Saldo = Überspeisung  (BKV hat mehr eingespeist als geplant)
 * Negativer Saldo = Unterdeckung  (BKV hat weniger eingespeist als geplant)
 *
 * Beide Abweichungen werden mit dem Ausgleichsenergiepreis bewertet.
 */
public record AusgleichsEnergieSaldo(
        String bilanzkreisId,
        Abrechnungsperiode periode,
        List<Double> saldoJeIntervallKwh,
        double ausgleichsenergiepreisEurKwh
) {
    public AusgleichsEnergieSaldo {
        Objects.requireNonNull(bilanzkreisId, "bilanzkreisId");
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(saldoJeIntervallKwh, "saldoJeIntervallKwh");
        if (ausgleichsenergiepreisEurKwh < 0)
            throw new IllegalArgumentException("Ausgleichsenergiepreis darf nicht negativ sein");
    }

    /** Gesamtabweichung in kWh über den Abrechnungszeitraum. */
    public double gesamtSaldoKwh() {
        return saldoJeIntervallKwh.stream().mapToDouble(Double::doubleValue).sum();
    }

    /** Gesamtkosten der Ausgleichsenergie in EUR. */
    public double ausgleichsenergiekostenEur() {
        double absoluteSumme = saldoJeIntervallKwh.stream()
                .mapToDouble(Math::abs)
                .sum();
        return absoluteSumme * ausgleichsenergiepreisEurKwh;
    }

    /** Maximale Viertelstundenabweichung (Spitze) in kWh. */
    public double spitzenabweichungKwh() {
        return saldoJeIntervallKwh.stream()
                .mapToDouble(Math::abs)
                .max()
                .orElse(0.0);
    }

    public boolean istImGleichgewicht() {
        return Math.abs(gesamtSaldoKwh()) < 0.001;
    }
}
