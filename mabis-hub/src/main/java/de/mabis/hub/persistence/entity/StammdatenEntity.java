package de.mabis.hub.persistence.entity;

import de.mabis.hub.domain.MaLoId;
import de.mabis.hub.domain.Stammdaten;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stammdaten")
public class StammdatenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "malo_id", nullable = false, length = 33)
    private String maloId;

    @Column(name = "netzgebiet", nullable = false)
    private String netzgebiet;

    @Column(name = "verfahren", nullable = false, length = 10)
    private String verfahren;

    @Column(name = "bilanzkreis_id", nullable = false, length = 50)
    private String bilanzkreisId;

    @Column(name = "gueltig_ab", nullable = false)
    private LocalDate gueltigAb;

    @Column(name = "gueltig_bis")
    private LocalDate gueltigBis;

    @Column(name = "erstellt_am", nullable = false)
    private LocalDateTime erstelltAm = LocalDateTime.now();

    protected StammdatenEntity() {}

    public static StammdatenEntity vonDomain(Stammdaten s) {
        StammdatenEntity e = new StammdatenEntity();
        e.maloId       = s.maloId().value();
        e.netzgebiet   = s.netzgebiet();
        e.verfahren    = s.verfahren().name();
        e.bilanzkreisId = s.bilanzkreisId();
        e.gueltigAb    = s.gueltigAb();
        e.gueltigBis   = s.gueltigBis();
        return e;
    }

    public Stammdaten zuDomain() {
        return new Stammdaten(
                MaLoId.of(maloId),
                netzgebiet,
                Stammdaten.Bilanzierungsverfahren.valueOf(verfahren),
                bilanzkreisId,
                gueltigAb,
                gueltigBis
        );
    }

    public String getMaloId()        { return maloId; }
    public String getBilanzkreisId() { return bilanzkreisId; }
    public LocalDate getGueltigAb()  { return gueltigAb; }
    public LocalDate getGueltigBis() { return gueltigBis; }
}
