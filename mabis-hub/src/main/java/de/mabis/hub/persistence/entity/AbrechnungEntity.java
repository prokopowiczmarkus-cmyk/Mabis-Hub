package de.mabis.hub.persistence.entity;

import de.mabis.hub.domain.*;
import de.mabis.hub.persistence.converter.DoubleListConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

@Entity
@Table(name = "abrechnungen")
public class AbrechnungEntity {

    @Id
    @Column(name = "abrechnungs_id", length = 20)
    private String abrechnungsId;

    @Column(name = "bilanzkreis_id", nullable = false, length = 50)
    private String bilanzkreisId;

    @Column(name = "bkv_id", nullable = false, length = 50)
    private String bkvId;

    @Column(name = "periode_monat", nullable = false, length = 7)
    private String periodeMonat;   // Format: YYYY-MM

    @Column(name = "periode_lauf", nullable = false, length = 15)
    private String periodeLauf;

    @Column(name = "ist_verbrauch_kwh", nullable = false)
    private double istVerbrauchKwh;

    @Column(name = "fahrplan_kwh", nullable = false)
    private double fahrplanKwh;

    @Convert(converter = DoubleListConverter.class)
    @Column(name = "saldo_werte_json", nullable = false, columnDefinition = "TEXT")
    private List<Double> saldoWerte;

    @Column(name = "ae_preis_eur_kwh", nullable = false)
    private double aePreisEurKwh;

    @Column(name = "netto_kosten_eur", nullable = false)
    private double nettoKostenEur;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    protected AbrechnungEntity() {}

    public static AbrechnungEntity vonDomain(Abrechnung a) {
        AbrechnungEntity e = new AbrechnungEntity();
        e.abrechnungsId  = a.abrechnungsId();
        e.bilanzkreisId  = a.bilanzkreisId();
        e.bkvId          = a.bkvId();
        e.periodeMonat   = a.periode().monat().toString();
        e.periodeLauf    = a.periode().lauf().name();
        e.istVerbrauchKwh = a.istVerbrauchKwh();
        e.fahrplanKwh    = a.fahrplanKwh();
        e.saldoWerte     = a.ausgleichsEnergieSaldo().saldoJeIntervallKwh();
        e.aePreisEurKwh  = a.ausgleichsEnergieSaldo().ausgleichsenergiepreisEurKwh();
        e.nettoKostenEur = a.nettoKostenEur();
        e.status         = a.status().name();
        e.erstelltAm     = a.erstelltAm();
        return e;
    }

    public Abrechnung zuDomain() {
        Abrechnungsperiode periode = new Abrechnungsperiode(
                YearMonth.parse(periodeMonat),
                Abrechnungsperiode.Lauf.valueOf(periodeLauf)
        );
        AusgleichsEnergieSaldo saldo = new AusgleichsEnergieSaldo(
                bilanzkreisId, periode, saldoWerte, aePreisEurKwh
        );
        return new Abrechnung(
                abrechnungsId, bilanzkreisId, bkvId, periode,
                istVerbrauchKwh, fahrplanKwh, saldo, nettoKostenEur,
                Abrechnung.AbrechnungsStatus.valueOf(status), erstelltAm
        );
    }

    public String getAbrechnungsId() { return abrechnungsId; }
    public String getBilanzkreisId() { return bilanzkreisId; }
}
