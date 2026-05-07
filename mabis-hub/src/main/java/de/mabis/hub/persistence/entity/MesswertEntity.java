package de.mabis.hub.persistence.entity;

import de.mabis.hub.domain.MaLoId;
import de.mabis.hub.domain.Messwert;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messwerte",
       uniqueConstraints = @UniqueConstraint(columnNames = {"malo_id", "zeitpunkt", "typ"}))
public class MesswertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "malo_id", nullable = false, length = 33)
    private String maloId;

    @Column(name = "zeitpunkt", nullable = false)
    private Instant zeitpunkt;

    @Column(name = "wert_kwh", nullable = false)
    private double wertKwh;

    @Column(name = "qualitaet", nullable = false, length = 15)
    private String qualitaet;

    @Column(name = "typ", nullable = false, length = 20)
    private String typ;

    @Column(name = "erstellt_am", nullable = false)
    private LocalDateTime erstelltAm = LocalDateTime.now();

    protected MesswertEntity() {}

    public static MesswertEntity vonDomain(Messwert m) {
        MesswertEntity e = new MesswertEntity();
        e.maloId    = m.maloId().value();
        e.zeitpunkt = m.zeitpunkt();
        e.wertKwh   = m.wertKwh();
        e.qualitaet = m.qualitaet().name();
        e.typ       = m.typ().name();
        return e;
    }

    public Messwert zuDomain() {
        return new Messwert(
                MaLoId.of(maloId),
                zeitpunkt,
                wertKwh,
                Messwert.Qualitaet.valueOf(qualitaet),
                Messwert.MesswertTyp.valueOf(typ)
        );
    }

    public String getMaloId()    { return maloId; }
    public Instant getZeitpunkt() { return zeitpunkt; }
    public double getWertKwh()   { return wertKwh; }
}
