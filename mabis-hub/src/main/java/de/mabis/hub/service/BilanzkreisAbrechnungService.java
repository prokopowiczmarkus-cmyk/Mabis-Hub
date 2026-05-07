package de.mabis.hub.service;

import de.mabis.hub.domain.*;
import de.mabis.hub.persistence.entity.AbrechnungEntity;
import de.mabis.hub.persistence.repository.AbrechnungRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
public class BilanzkreisAbrechnungService {

    private static final Logger LOG = Logger.getLogger(BilanzkreisAbrechnungService.class.getName());
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final AggregationsService aggregationsService;
    private final FahrplanService fahrplanService;
    private final AbrechnungRepository repository;

    public BilanzkreisAbrechnungService(AggregationsService aggregationsService,
                                         FahrplanService fahrplanService,
                                         AbrechnungRepository repository) {
        this.aggregationsService = aggregationsService;
        this.fahrplanService     = fahrplanService;
        this.repository          = repository;
    }

    public Abrechnung abrechnungslaufDurchfuehren(String bilanzkreisId,
                                                   YearMonth yearMonth,
                                                   Abrechnungsperiode.Lauf lauf,
                                                   String netzgebiet,
                                                   double aePreisEurKwh) {
        Abrechnungsperiode periode = new Abrechnungsperiode(yearMonth, lauf);
        LOG.info("Abrechnungslauf gestartet: " + periode + " | BK: " + bilanzkreisId);

        AggregierterLastgang istLastgang = aggregationsService.aggregieren(
                bilanzkreisId, periode.von(), periode.bis(), netzgebiet);

        List<Double> fahrplanWerte = fahrplanWerteMonat(bilanzkreisId, yearMonth);
        double fahrplanGesamtKwh   = fahrplanWerte.stream().mapToDouble(Double::doubleValue).sum();

        AusgleichsEnergieSaldo saldo = saldoBerechnen(
                bilanzkreisId, periode, istLastgang, fahrplanWerte, aePreisEurKwh);

        String abrechnungsId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Abrechnung abrechnung = new Abrechnung(
                abrechnungsId, bilanzkreisId, "BKV-" + bilanzkreisId, periode,
                istLastgang.gesamtverbrauchKwh(), fahrplanGesamtKwh, saldo,
                saldo.ausgleichsenergiekostenEur(),
                Abrechnung.AbrechnungsStatus.ERSTELLT, Instant.now());

        repository.save(AbrechnungEntity.vonDomain(abrechnung));

        LOG.info("Abrechnung persistiert: " + abrechnung);
        return abrechnung;
    }

    @Transactional(readOnly = true)
    public Optional<Abrechnung> abrechnungLaden(String abrechnungsId) {
        return repository.findById(abrechnungsId).map(AbrechnungEntity::zuDomain);
    }

    @Transactional(readOnly = true)
    public List<Abrechnung> alleAbrechnungen(String bilanzkreisId) {
        return repository.findByBilanzkreisIdOrderByErstelltAmDesc(bilanzkreisId)
                .stream()
                .map(AbrechnungEntity::zuDomain)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaLoId> fehlendeMalos(String bilanzkreisId, LocalDate stichtag,
                                       Instant von, Instant bis) {
        return aggregationsService.fehlendeMalos(bilanzkreisId, stichtag, von, bis);
    }

    private AusgleichsEnergieSaldo saldoBerechnen(String bilanzkreisId,
                                                   Abrechnungsperiode periode,
                                                   AggregierterLastgang istLastgang,
                                                   List<Double> fahrplanWerte,
                                                   double aePreisEurKwh) {
        int anzahl = Math.min(istLastgang.viertelstundenwerte().size(), fahrplanWerte.size());
        List<Double> saldoWerte = new ArrayList<>(anzahl);
        for (int i = 0; i < anzahl; i++) {
            saldoWerte.add(istLastgang.viertelstundenwerte().get(i)
                         - (i < fahrplanWerte.size() ? fahrplanWerte.get(i) : 0.0));
        }
        return new AusgleichsEnergieSaldo(bilanzkreisId, periode, saldoWerte, aePreisEurKwh);
    }

    private List<Double> fahrplanWerteMonat(String bilanzkreisId, YearMonth yearMonth) {
        List<Double> monatsWerte = new ArrayList<>();
        for (LocalDate tag = yearMonth.atDay(1); !tag.isAfter(yearMonth.atEndOfMonth());
                tag = tag.plusDays(1)) {
            fahrplanService.fahrplanLaden(bilanzkreisId, tag)
                    .ifPresentOrElse(
                        fp -> monatsWerte.addAll(fp.viertelstundenwerteKwh()),
                        () -> {
                            LOG.warning("Kein Fahrplan für " + bilanzkreisId + " am " + tag);
                            for (int i = 0; i < Fahrplan.INTERVALLE_PRO_TAG; i++)
                                monatsWerte.add(0.0);
                        });
        }
        return monatsWerte;
    }
}
