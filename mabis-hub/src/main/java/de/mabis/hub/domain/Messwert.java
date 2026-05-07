package de.mabis.hub.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Ein Messwert einer Marktlokation, referenziert ausschließlich über MaLo-ID.
 * Einheit: kWh, Auflösung: 15-Minuten-Intervall (Lastgang) oder Zählerstand.
 */
public record Messwert(
        MaLoId maloId,
        Instant zeitpunkt,
        double wertKwh,
        Qualitaet qualitaet,
        MesswertTyp typ
) {
    public Messwert {
        Objects.requireNonNull(maloId, "maloId");
        Objects.requireNonNull(zeitpunkt, "zeitpunkt");
        Objects.requireNonNull(qualitaet, "qualitaet");
        Objects.requireNonNull(typ, "typ");
        if (wertKwh < 0) throw new IllegalArgumentException("Messwert darf nicht negativ sein");
    }

    public enum Qualitaet {
        GEMESSEN,       // Realer Messwert vom Zähler
        ERSATZWERT,     // Vom MSB gebildeter Ersatzwert
        PROGNOSEWERT    // Für Bilanzierungszwecke geschätzter Wert
    }

    public enum MesswertTyp {
        LASTGANG_15MIN,     // Viertelstunden-Lastgang (RLM-Kunden)
        ZAEHLERSTAND,       // Zählerstand (SLP-Kunden)
        TAGESGANG           // Tageswert
    }

    public boolean istErsatzwert() {
        return qualitaet == Qualitaet.ERSATZWERT;
    }
}
