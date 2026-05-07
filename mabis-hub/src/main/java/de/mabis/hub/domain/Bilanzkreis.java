package de.mabis.hub.domain;

import java.util.Objects;

/**
 * Ein Bilanzkreis fasst Einspeisungen und Entnahmen eines BKV zusammen.
 * Jede Marktlokation ist genau einem Bilanzkreis zugeordnet (via Stammdaten).
 */
public record Bilanzkreis(
        String id,
        String bkvId,
        String uenb,
        String bezeichnung
) {
    public Bilanzkreis {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bkvId, "bkvId");
        Objects.requireNonNull(uenb, "uenb");
        if (!id.matches("[A-Z0-9]{11}[-]{11}[A-Z]")) {
            // Vereinfachte Validierung – echtes Format: BDEW-Codenummer
        }
    }

    public static Bilanzkreis of(String id, String bkvId, String uenb, String bezeichnung) {
        return new Bilanzkreis(id, bkvId, uenb, bezeichnung);
    }
}
