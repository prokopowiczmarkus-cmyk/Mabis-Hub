package de.mabis.hub.api.dto;

import de.mabis.hub.domain.Abrechnung;

import java.time.Instant;
import java.time.YearMonth;

public record AbrechnungResponse(
        String abrechnungsId,
        String bilanzkreisId,
        String bkvId,
        String periode,
        String lauf,
        YearMonth faelligkeit,
        double istVerbrauchKwh,
        double fahrplanKwh,
        double abweichungKwh,
        double abweichungProzent,
        double ausgleichsenergiekostenEur,
        double spitzenabweichungKwh,
        String status,
        Instant erstelltAm
) {
    public static AbrechnungResponse von(Abrechnung a) {
        return new AbrechnungResponse(
                a.abrechnungsId(),
                a.bilanzkreisId(),
                a.bkvId(),
                a.periode().toString(),
                a.periode().lauf().name(),
                a.periode().faelligkeit(),
                a.istVerbrauchKwh(),
                a.fahrplanKwh(),
                a.abweichungKwh(),
                Math.round(a.abweichungProzent() * 100.0) / 100.0,
                a.ausgleichsEnergieSaldo().ausgleichsenergiekostenEur(),
                a.ausgleichsEnergieSaldo().spitzenabweichungKwh(),
                a.status().name(),
                a.erstelltAm()
        );
    }
}
