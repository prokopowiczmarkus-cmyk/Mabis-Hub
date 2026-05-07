package de.mabis.hub.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StammdatenRequest(

        @NotBlank(message = "maloId ist Pflichtfeld")
        String maloId,

        @NotBlank(message = "netzgebiet ist Pflichtfeld")
        String netzgebiet,

        @NotBlank(message = "verfahren ist Pflichtfeld (SLP | RLM | iMSys)")
        String verfahren,

        @NotBlank(message = "bilanzkreisId ist Pflichtfeld")
        String bilanzkreisId,

        @NotNull(message = "gueltigAb ist Pflichtfeld")
        LocalDate gueltigAb,

        LocalDate gueltigBis
) {}
