package de.mabis.hub.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Pseudonymisierte Marktlokations-ID (MaLo-ID).
 * Der Hub kennt ausschließlich diese ID – nie den dahinterstehenden Letztverbraucher.
 * Gemäß BK6-24-210-1: Pseudonymisierungspflicht für alle Messwertverarbeitungsprozesse.
 */
public record MaLoId(String value) {

    public MaLoId {
        Objects.requireNonNull(value, "MaLo-ID darf nicht null sein");
        if (!value.matches("DE\\d{11}[A-Z0-9]{20}")) {
            throw new IllegalArgumentException("Ungültiges MaLo-ID-Format: " + value);
        }
    }

    public static MaLoId of(String value) {
        return new MaLoId(value);
    }

    /** Erzeugt eine synthetisch valide Test-MaLo-ID. */
    public static MaLoId random() {
        String uid = UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 20);
        return new MaLoId("DE00000000001" + uid);
    }

    @Override
    public String toString() {
        return value;
    }
}
