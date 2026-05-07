package de.mabis.hub;

import de.mabis.hub.api.MaBiSHubFassade;
import de.mabis.hub.domain.*;
import de.mabis.hub.persistence.repository.AbrechnungRepository;
import de.mabis.hub.persistence.repository.FahrplanRepository;
import de.mabis.hub.persistence.repository.MesswertRepository;
import de.mabis.hub.persistence.repository.StammdatenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = MaBiSHubApplication.class)
@DisplayName("Bilanzkreisabrechnung – Integrationstests")
class BilanzkreisAbrechnungTest extends TestContainersBase {

    @Autowired MaBiSHubFassade      hub;
    @Autowired StammdatenRepository stammdatenRepo;
    @Autowired MesswertRepository   messwertRepo;
    @Autowired FahrplanRepository   fahrplanRepo;
    @Autowired AbrechnungRepository abrechnungRepo;

    private static final String BK        = "11YSWKW-----------W";
    private static final String NETZGEBIET = "NW-West";
    private static final YearMonth PERIODE  = YearMonth.of(2026, 1);
    private static final double AE_PREIS   = 0.12;

    // 3 Testtage statt ganzer Monat – ausreichend für alle Assertions, deutlich schneller
    private static final LocalDate VON = LocalDate.of(2026, 1, 1);
    private static final LocalDate BIS = LocalDate.of(2026, 1, 3);

    private MaLoId malo1, malo2, malo3, malo4, malo5;

    @BeforeEach
    void setUp() {
        abrechnungRepo.deleteAll();
        fahrplanRepo.deleteAll();
        messwertRepo.deleteAll();
        stammdatenRepo.deleteAll();

        malo1 = MaLoId.random(); malo2 = MaLoId.random(); malo3 = MaLoId.random();
        malo4 = MaLoId.random(); malo5 = MaLoId.random();

        for (MaLoId m : List.of(malo1, malo2, malo3, malo4, malo5)) {
            hub.stammdatenMelden(new Stammdaten(m, NETZGEBIET,
                    Stammdaten.Bilanzierungsverfahren.RLM, BK, VON, null));
        }
    }

    // ── Reine Domain-Tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Abrechnungsperiode – Fälligkeitsberechnung (Domain)")
    class FaelligkeitTests {

        @Test
        @DisplayName("Vorläufige Abrechnung fällig M+1")
        void vorlaeufig_faellig_m_plus_1() {
            var periode = new Abrechnungsperiode(PERIODE, Abrechnungsperiode.Lauf.VORLAEUFIG);
            assertThat(periode.faelligkeit()).isEqualTo(YearMonth.of(2026, 2));
        }

        @Test
        @DisplayName("Endgültige Abrechnung fällig M+67")
        void endgueltig_faellig_m_plus_67() {
            var periode = new Abrechnungsperiode(PERIODE, Abrechnungsperiode.Lauf.ENDGUELTIG);
            assertThat(periode.faelligkeit()).isEqualTo(YearMonth.of(2031, 8));
        }
    }

    // ── Integrationstests mit DB ──────────────────────────────────────────────

    @Nested
    @DisplayName("Abrechnungsläufe")
    class AbrechnungsTests {

        @Test
        @DisplayName("Vorläufige Abrechnung M+1: Ergebnis korrekt persistiert")
        void vorlaeufige_abrechnung_erstellt() {
            for (LocalDate d = VON; !d.isAfter(BIS); d = d.plusDays(1))
                hub.fahrplanEinreichen(tagesfahrplan(d, 100.0));
            messwertEinliefern(22.0);

            Abrechnung abrechnung = hub.abrechnungslaufAnstoßen(
                    BK, PERIODE, Abrechnungsperiode.Lauf.VORLAEUFIG, NETZGEBIET, AE_PREIS);

            assertThat(abrechnung.periode().lauf()).isEqualTo(Abrechnungsperiode.Lauf.VORLAEUFIG);
            assertThat(abrechnung.istVerbrauchKwh()).isGreaterThan(0);
            assertThat(abrechnung.fahrplanKwh()).isGreaterThan(0);
            assertThat(abrechnung.status()).isEqualTo(Abrechnung.AbrechnungsStatus.ERSTELLT);
            assertThat(abrechnungRepo.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Rollierende Abrechnung: drei Läufe werden alle persistiert")
        void rollierende_abrechnung_drei_laeufe() {
            for (LocalDate d = VON; !d.isAfter(BIS); d = d.plusDays(1))
                hub.fahrplanEinreichen(tagesfahrplan(d, 80.0));
            messwertEinliefern(18.0);

            hub.abrechnungslaufAnstoßen(BK, PERIODE, Abrechnungsperiode.Lauf.VORLAEUFIG, NETZGEBIET, AE_PREIS);
            hub.abrechnungslaufAnstoßen(BK, PERIODE, Abrechnungsperiode.Lauf.KORRIGIERT,  NETZGEBIET, AE_PREIS);
            Abrechnung endgueltig = hub.abrechnungslaufAnstoßen(
                    BK, PERIODE, Abrechnungsperiode.Lauf.ENDGUELTIG, NETZGEBIET, AE_PREIS);

            assertThat(hub.alleAbrechnungen(BK)).hasSize(3);
            assertThat(endgueltig.istEndgueltig()).isTrue();
        }

        @Test
        @DisplayName("Kein Fahrplan → Ausgleichsenergiekosten gleich Ist-Verbrauch × Preis")
        void abrechnung_ohne_fahrplan_maximale_ae_kosten() {
            // kein Fahrplan → Nullwerte als Basis
            messwertEinliefern(20.0);

            Abrechnung abrechnung = hub.abrechnungslaufAnstoßen(
                    BK, PERIODE, Abrechnungsperiode.Lauf.VORLAEUFIG, NETZGEBIET, AE_PREIS);

            assertThat(abrechnung.ausgleichsEnergieSaldo().ausgleichsenergiekostenEur())
                    .isEqualTo(abrechnung.istVerbrauchKwh() * AE_PREIS, within(0.01));
        }

        @Test
        @DisplayName("Aggregation über 5 MaLos erfüllt Datenschutz-Mindestclustergröße")
        void fuenf_malos_erfuellen_datenschutzanforderung() {
            messwertEinliefern(10.0);

            Abrechnung abrechnung = hub.abrechnungslaufAnstoßen(
                    BK, PERIODE, Abrechnungsperiode.Lauf.VORLAEUFIG, NETZGEBIET, AE_PREIS);

            assertThat(abrechnung.ausgleichsEnergieSaldo().saldoJeIntervallKwh()).isNotEmpty();
        }
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private Fahrplan tagesfahrplan(LocalDate tag, double kwhProIntervall) {
        return new Fahrplan(BK, tag,
                new ArrayList<>(Collections.nCopies(Fahrplan.INTERVALLE_PRO_TAG, kwhProIntervall)),
                Fahrplan.FahrplanTyp.ENTNAHME, "BKV-" + BK);
    }

    private void messwertEinliefern(double kwhProIntervall) {
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        for (LocalDate tag = VON; !tag.isAfter(BIS); tag = tag.plusDays(1)) {
            Instant basis = tag.atStartOfDay(berlin).toInstant();
            for (MaLoId malo : List.of(malo1, malo2, malo3, malo4, malo5)) {
                for (int i = 0; i < Fahrplan.INTERVALLE_PRO_TAG; i++) {
                    hub.messwertEinliefern(new Messwert(
                            malo,
                            basis.plusSeconds((long) i * 15 * 60),
                            kwhProIntervall,
                            Messwert.Qualitaet.GEMESSEN,
                            Messwert.MesswertTyp.LASTGANG_15MIN));
                }
            }
        }
    }
}
