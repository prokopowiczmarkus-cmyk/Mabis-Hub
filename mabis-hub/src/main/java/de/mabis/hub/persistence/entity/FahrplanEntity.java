package de.mabis.hub.persistence.entity;

import de.mabis.hub.domain.Fahrplan;
import de.mabis.hub.persistence.converter.DoubleListConverter;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fahrplaene",
       uniqueConstraints = @UniqueConstraint(columnNames = {"bilanzkreis_id", "liefertag"}))
public class FahrplanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bilanzkreis_id", nullable = false, length = 50)
    private String bilanzkreisId;

    @Column(name = "liefertag", nullable = false)
    private LocalDate liefertag;

    @Convert(converter = DoubleListConverter.class)
    @Column(name = "viertelstundenwerte_json", nullable = false, columnDefinition = "TEXT")
    private List<Double> viertelstundenwerteKwh;

    @Column(name = "typ", nullable = false, length = 15)
    private String typ;

    @Column(name = "bkv_id", nullable = false, length = 50)
    private String bkvId;

    @Column(name = "eingereicht_am", nullable = false)
    private LocalDateTime eingereichtAm = LocalDateTime.now();

    protected FahrplanEntity() {}

    public static FahrplanEntity vonDomain(Fahrplan f) {
        FahrplanEntity e = new FahrplanEntity();
        e.bilanzkreisId          = f.bilanzkreisId();
        e.liefertag              = f.liefertag();
        e.viertelstundenwerteKwh = f.viertelstundenwerteKwh();
        e.typ                    = f.typ().name();
        e.bkvId                  = f.bkvId();
        return e;
    }

    public Fahrplan zuDomain() {
        return new Fahrplan(
                bilanzkreisId,
                liefertag,
                viertelstundenwerteKwh,
                Fahrplan.FahrplanTyp.valueOf(typ),
                bkvId
        );
    }

    public String getBilanzkreisId()           { return bilanzkreisId; }
    public LocalDate getLiefertag()            { return liefertag; }
    public List<Double> getViertelstundenwerteKwh() { return viertelstundenwerteKwh; }
}
