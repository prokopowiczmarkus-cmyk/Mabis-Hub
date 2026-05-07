package de.mabis.hub.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bilanzierungsrelevante Stammdaten einer Marktlokation.
 * Geliefert vom Verteilnetzbetreiber (VNB) – ausschließlich unter MaLo-ID-Referenz.
 */
public record Stammdaten(
        MaLoId maloId,
        String netzgebiet,
        Bilanzierungsverfahren verfahren,
        String bilanzkreisId,
        LocalDate gueltigAb,
        LocalDate gueltigBis
) {
    public Stammdaten {
        Objects.requireNonNull(maloId, "maloId");
        Objects.requireNonNull(netzgebiet, "netzgebiet");
        Objects.requireNonNull(verfahren, "verfahren");
        Objects.requireNonNull(bilanzkreisId, "bilanzkreisId");
        Objects.requireNonNull(gueltigAb, "gueltigAb");
    }

    public enum Bilanzierungsverfahren {
        SLP,        // Standardlastprofil (Haushaltskunden)
        RLM,        // Registrierende Leistungsmessung (Gewerbe/Industrie)
        iMSys       // Intelligentes Messsystem (Smart Meter)
    }

    public boolean istAktiv(LocalDate stichtag) {
        return !stichtag.isBefore(gueltigAb)
                && (gueltigBis == null || !stichtag.isAfter(gueltigBis));
    }
}
