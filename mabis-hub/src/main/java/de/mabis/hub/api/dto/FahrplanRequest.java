package de.mabis.hub.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record FahrplanRequest(

        @NotBlank(message = "bilanzkreisId ist Pflichtfeld")
        String bilanzkreisId,

        @NotNull(message = "liefertag ist Pflichtfeld")
        LocalDate liefertag,

        @NotNull
        @Size(min = 96, max = 96, message = "Fahrplan muss exakt 96 Viertelstundenwerte enthalten")
        List<Double> viertelstundenwerteKwh,

        @NotBlank(message = "typ ist Pflichtfeld (EINSPEISUNG | ENTNAHME)")
        String typ,

        @NotBlank(message = "bkvId ist Pflichtfeld")
        String bkvId
) {}
