package de.mabis.hub.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.YearMonth;

public record AbrechnungslaufRequest(

        @NotBlank(message = "bilanzkreisId ist Pflichtfeld")
        String bilanzkreisId,

        @NotNull(message = "yearMonth ist Pflichtfeld (Format: YYYY-MM)")
        YearMonth yearMonth,

        @NotBlank(message = "lauf ist Pflichtfeld (VORLAEUFIG | KORRIGIERT | ENDGUELTIG)")
        String lauf,

        @NotBlank(message = "netzgebiet ist Pflichtfeld")
        String netzgebiet,

        @Positive(message = "aePreisEurKwh muss größer 0 sein")
        double aePreisEurKwh
) {}
