package de.mabis.hub.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Unveränderlicher Audit-Log-Eintrag.
 * Repräsentiert eine einzelne protokollierte Operation im MaBiS-Hub.
 */
public record AuditEintrag(
        UUID id,
        AuditEreignis ereignis,
        String marktRolle,
        String principalId,
        String maloId,
        String bilanzkreisId,
        String details,
        boolean erfolg,
        String fehlerMeldung,
        Instant zeitpunkt,
        String anfrageIp
) {
    public AuditEintrag {
        Objects.requireNonNull(ereignis,  "ereignis");
        Objects.requireNonNull(zeitpunkt, "zeitpunkt");
    }

    public static Builder builder(AuditEreignis ereignis) {
        return new Builder(ereignis);
    }

    public static final class Builder {
        private final AuditEreignis ereignis;
        private String marktRolle;
        private String principalId;
        private String maloId;
        private String bilanzkreisId;
        private String details;
        private boolean erfolg = true;
        private String fehlerMeldung;
        private String anfrageIp;

        private Builder(AuditEreignis ereignis) {
            this.ereignis = ereignis;
        }

        public Builder marktRolle(String v)      { this.marktRolle = v;      return this; }
        public Builder principalId(String v)     { this.principalId = v;     return this; }
        public Builder maloId(String v)          { this.maloId = v;          return this; }
        public Builder bilanzkreisId(String v)   { this.bilanzkreisId = v;   return this; }
        public Builder details(String v)         { this.details = v;         return this; }
        public Builder erfolg(boolean v)         { this.erfolg = v;          return this; }
        public Builder fehlerMeldung(String v)   { this.fehlerMeldung = v; this.erfolg = false; return this; }
        public Builder anfrageIp(String v)       { this.anfrageIp = v;       return this; }

        public AuditEintrag build() {
            return new AuditEintrag(UUID.randomUUID(), ereignis, marktRolle, principalId,
                    maloId, bilanzkreisId, details, erfolg, fehlerMeldung,
                    Instant.now(), anfrageIp);
        }
    }
}
