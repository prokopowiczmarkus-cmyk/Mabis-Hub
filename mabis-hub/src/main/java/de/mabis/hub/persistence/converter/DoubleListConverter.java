package de.mabis.hub.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Konvertiert List<Double> ↔ CSV-String für die Datenbankspeicherung.
 * Wird für Fahrplan-Viertelstundenwerte und Abrechnungssalden verwendet.
 */
@Converter
public class DoubleListConverter implements AttributeConverter<List<Double>, String> {

    @Override
    public String convertToDatabaseColumn(List<Double> werte) {
        if (werte == null || werte.isEmpty()) return "";
        return werte.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Override
    public List<Double> convertToEntityAttribute(String dbWert) {
        if (dbWert == null || dbWert.isBlank()) return Collections.emptyList();
        return Arrays.stream(dbWert.split(","))
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }
}
