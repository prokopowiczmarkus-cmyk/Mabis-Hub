package de.mabis.hub.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditEntity {

    @Id
    private UUID id;

    @Column(name = "ereignis", nullable = false, length = 60)
    private String ereignis;

    @Column(name = "markt_rolle", length = 20)
    private String marktRolle;

    @Column(name = "principal_id", length = 150)
    private String principalId;

    @Column(name = "malo_id", length = 33)
    private String maloId;

    @Column(name = "bilanzkreis_id", length = 50)
    private String bilanzkreisId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "erfolg", nullable = false)
    private boolean erfolg;

    @Column(name = "fehler_meldung", columnDefinition = "TEXT")
    private String fehlerMeldung;

    @Column(name = "zeitpunkt", nullable = false)
    private Instant zeitpunkt;

    @Column(name = "anfrage_ip", length = 50)
    private String anfrageIp;

    protected AuditEntity() {}

    public static AuditEntity vonDomain(AuditEintrag e) {
        AuditEntity entity = new AuditEntity();
        entity.id             = e.id();
        entity.ereignis       = e.ereignis().name();
        entity.marktRolle     = e.marktRolle();
        entity.principalId    = e.principalId();
        entity.maloId         = e.maloId();
        entity.bilanzkreisId  = e.bilanzkreisId();
        entity.details        = e.details();
        entity.erfolg         = e.erfolg();
        entity.fehlerMeldung  = e.fehlerMeldung();
        entity.zeitpunkt      = e.zeitpunkt();
        entity.anfrageIp      = e.anfrageIp();
        return entity;
    }

    public AuditEintrag zuDomain() {
        return new AuditEintrag(id, AuditEreignis.valueOf(ereignis), marktRolle, principalId,
                maloId, bilanzkreisId, details, erfolg, fehlerMeldung, zeitpunkt, anfrageIp);
    }

    // Getter für Projektionen
    public UUID getId()           { return id; }
    public String getEreignis()   { return ereignis; }
    public Instant getZeitpunkt() { return zeitpunkt; }
    public boolean isErfolg()     { return erfolg; }
}
