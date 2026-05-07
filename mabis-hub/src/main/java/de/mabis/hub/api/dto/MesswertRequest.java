package de.mabis.hub.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record MesswertRequest(

        @NotBlank(message = "maloId darf nicht leer sein")
        String maloId,

        @NotNull(message = "zeitpunkt ist Pflichtfeld")
        Instant zeitpunkt,

        @PositiveOrZero(message = "wertKwh darf nicht negativ sein")
        double wertKwh,

        @NotBlank(message = "qualitaet ist Pflichtfeld (GEMESSEN | ERSATZWERT | PROGNOSEWERT)")
        String qualitaet,

        @NotBlank(message = "typ ist Pflichtfeld (LASTGANG_15MIN | ZAEHLERSTAND | TAGESGANG)")
        String typ
) {}
