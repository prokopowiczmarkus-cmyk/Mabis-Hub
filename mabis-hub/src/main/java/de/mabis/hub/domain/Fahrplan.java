package de.mabis.hub.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Energiefahrplan des Bilanzkreisverantwortlichen (BKV) für einen Tag.
 * Der BKV prognostiziert darin seine geplante Einspeisung und Entnahme
 * in 15-Minuten-Intervallen (96 Werte pro Tag).
 */
public record Fahrplan(
        String bilanzkreisId,
        LocalDate liefertag,
        List<Double> viertelstundenwerteKwh,
        FahrplanTyp typ,
        String bkvId
) {
    public static final int INTERVALLE_PRO_TAG = 96;

    public Fahrplan {
        Objects.requireNonNull(bilanzkreisId, "bilanzkreisId");
        Objects.requireNonNull(liefertag, "liefertag");
        Objects.requireNonNull(viertelstundenwerteKwh, "viertelstundenwerteKwh");
        Objects.requireNonNull(typ, "typ");
        if (viertelstundenwerteKwh.size() != INTERVALLE_PRO_TAG) {
            throw new IllegalArgumentException(
                "Fahrplan muss exakt " + INTERVALLE_PRO_TAG + " Viertelstundenwerte enthalten, "
                + "erhalten: " + viertelstundenwerteKwh.size());
        }
    }

    public enum FahrplanTyp {
        EINSPEISUNG,   // Geplante Einspeisung ins Netz
        ENTNAHME       // Geplante Entnahme aus dem Netz
    }

    public double gesamtKwh() {
        return viertelstundenwerteKwh.stream().mapToDouble(Double::doubleValue).sum();
    }

    public double wertZuIntervall(int intervall) {
        if (intervall < 0 || intervall >= INTERVALLE_PRO_TAG)
            throw new IndexOutOfBoundsException("Intervall muss zwischen 0 und 95 liegen");
        return viertelstundenwerteKwh.get(intervall);
    }
}
