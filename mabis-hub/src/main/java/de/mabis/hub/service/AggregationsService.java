package de.mabis.hub.service;

import de.mabis.hub.domain.AggregierterLastgang;
import de.mabis.hub.domain.MaLoId;
import de.mabis.hub.domain.Messwert;
import de.mabis.hub.domain.Stammdaten;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Zentraler Aggregationsservice des MaBiS-Hubs.
 *
 * Kernaufgabe gemäß BK6-24-210: Zusammenführung von Messwerten aller
 * Marktlokationen eines Bilanzkreises zu aggregierten Lastgängen für die
 * Bilanzkreisabrechnung. Ablösung der dezentralen ÜNB-Aggregation.
 */
public class AggregationsService {

    private static final Logger LOG = Logger.getLogger(AggregationsService.class.getName());

    private final StammdatenService stammdatenService;
    private final MesswertVerarbeitungService messwertService;

    public AggregationsService(StammdatenService stammdatenService,
                                MesswertVerarbeitungService messwertService) {
        this.stammdatenService = stammdatenService;
        this.messwertService = messwertService;
    }

    /**
     * Aggregiert alle Messwerte eines Bilanzkreises für einen Abrechnungszeitraum.
     * Ergebnis: Viertelstunden-Summenlastgang über alle MaLo-IDs des Bilanzkreises.
     */
    public AggregierterLastgang aggregieren(String bilanzkreisId, LocalDate von, LocalDate bis,
                                             String netzgebiet) {
        Instant vonInstant = von.atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant();
        Instant bisInstant = bis.atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant();

        List<MaLoId> malos = stammdatenService.malosImBilanzkreis(bilanzkreisId, von);
        LOG.info("Aggregation gestartet | Bilanzkreis: " + bilanzkreisId
                + " | Anzahl MaLos: " + malos.size());

        int intervalle = (int) ((bisInstant.getEpochSecond() - vonInstant.getEpochSecond()) / (15 * 60));
        double[] summenwerte = new double[intervalle];

        for (MaLoId malo : malos) {
            List<Messwert> messwerte = messwertService.messwerteLaden(malo, vonInstant, bisInstant);
            for (int i = 0; i < Math.min(messwerte.size(), intervalle); i++) {
                summenwerte[i] += messwerte.get(i).wertKwh();
            }
        }

        List<Double> aggregiert = new ArrayList<>();
        for (double w : summenwerte) aggregiert.add(w);

        AggregierterLastgang ergebnis = new AggregierterLastgang(
                bilanzkreisId, von, bis, aggregiert, malos.size(), netzgebiet);

        if (!ergebnis.erfuelltDatenschutzanforderung()) {
            LOG.warning("Datenschutzwarnung: Bilanzkreis " + bilanzkreisId
                    + " hat weniger als " + AggregierterLastgang.MIN_CLUSTER_GROESSE
                    + " MaLos – Pseudonymisierungsanforderung prüfen!");
        }

        LOG.info("Aggregation abgeschlossen | Gesamtverbrauch: "
                + String.format("%.2f", ergebnis.gesamtverbrauchKwh()) + " kWh"
                + " | Spitzenlast: " + String.format("%.2f", ergebnis.spitzenlastKw()) + " kW");

        return ergebnis;
    }

    /**
     * Prüft Vollständigkeit aller MaLo-Messwerte vor der Abrechnung.
     * Gibt MaLo-IDs mit Datenlücken zurück.
     */
    public List<MaLoId> fehlendeMalos(String bilanzkreisId, LocalDate stichtag,
                                       Instant von, Instant bis) {
        return stammdatenService.malosImBilanzkreis(bilanzkreisId, stichtag)
                .stream()
                .filter(malo -> !messwertService.istVollstaendig(malo, von, bis))
                .collect(Collectors.toList());
    }
}
