package de.mabis.hub;

import de.mabis.hub.api.MaBiSHubFassade;
import de.mabis.hub.domain.*;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = MaBiSHubApplication.class)
@DisplayName("MaBiS-Hub Integrationstests")
class MaBiSHubIntegrationTest extends TestContainersBase {

    @Autowired MaBiSHubFassade      hub;
    @Autowired StammdatenRepository stammdatenRepo;
    @Autowired MesswertRepository   messwertRepo;

    private MaLoId malo1;
    private MaLoId malo2;

    private static final String BK        = "11YSWKW-----------W";
    private static final String NETZGEBIET = "Netzgebiet-West";
    private static final LocalDate STICHTAG = LocalDate.of(2026, 1, 15);

    @BeforeEach
    void setUp() {
        messwertRepo.deleteAll();
        stammdatenRepo.deleteAll();
        malo1 = MaLoId.random();
        malo2 = MaLoId.random();
    }

    // ── Reine Domain-Tests (kein DB-Zugriff) ─────────────────────────────────

    @Nested
    @DisplayName("Domain-Logik")
    class DomainTests {

        @Test
        @DisplayName("Datenschutzwarnung: Cluster unter 5 MaLos verletzt Mindestanforderung")
        void datenschutzClusterGroesse() {
            AggregierterLastgang lastgang = new AggregierterLastgang(
                    BK, STICHTAG, STICHTAG.plusDays(1),
                    List.of(10.0, 12.0, 11.5), 2, NETZGEBIET);

            assertThat(lastgang.erfuelltDatenschutzanforderung()).isFalse();
        }

        @Test
        @DisplayName("Cluster mit 5 MaLos erfüllt Datenschutzanforderung")
        void datenschutzClusterAusreichend() {
            AggregierterLastgang lastgang = new AggregierterLastgang(
                    BK, STICHTAG, STICHTAG.plusDays(1),
                    List.of(10.0, 12.0, 11.5), 5, NETZGEBIET);

            assertThat(lastgang.erfuelltDatenschutzanforderung()).isTrue();
        }
    }

    // ── Integrationstests mit DB ──────────────────────────────────────────────

    @Nested
    @DisplayName("Hub-Prozessabläufe")
    class ProzessTests {

        @Test
        @DisplayName("VNB meldet Stammdaten – Hub erkennt MaLo als bekannt")
        void stammdatenRegistrierung() {
            hub.stammdatenMelden(new Stammdaten(malo1, NETZGEBIET,
                    Stammdaten.Bilanzierungsverfahren.SLP, BK,
                    LocalDate.of(2026, 1, 1), null));

            assertThat(hub.status().registrierteMalos()).isEqualTo(1);
        }

        @Test
        @DisplayName("MSB kann keine Messwerte ohne vorherige Stammdaten einliefern")
        void messwertOhneStammdatenWirdAbgelehnt() {
            Messwert messwert = new Messwert(malo1, Instant.now(), 1.5,
                    Messwert.Qualitaet.GEMESSEN, Messwert.MesswertTyp.LASTGANG_15MIN);

            assertThatThrownBy(() -> hub.messwertEinliefern(messwert))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Stammdaten zuerst übermitteln");
        }

        @Test
        @DisplayName("Vollständiger Prozessablauf: Stammdaten → Messwerte → Aggregation")
        void vollstaendigerProzessablauf() {
            // VNB meldet Stammdaten
            hub.stammdatenMelden(new Stammdaten(malo1, NETZGEBIET,
                    Stammdaten.Bilanzierungsverfahren.RLM, BK,
                    LocalDate.of(2026, 1, 1), null));
            hub.stammdatenMelden(new Stammdaten(malo2, NETZGEBIET,
                    Stammdaten.Bilanzierungsverfahren.RLM, BK,
                    LocalDate.of(2026, 1, 1), null));

            // MSB liefert 4 Viertelstundenmesswerte je MaLo
            Instant basis = STICHTAG.atStartOfDay(java.time.ZoneId.of("Europe/Berlin")).toInstant();
            for (int i = 0; i < 4; i++) {
                hub.messwertEinliefern(new Messwert(malo1,
                        basis.plusSeconds((long) i * 15 * 60), 10.0 + i,
                        Messwert.Qualitaet.GEMESSEN, Messwert.MesswertTyp.LASTGANG_15MIN));
                hub.messwertEinliefern(new Messwert(malo2,
                        basis.plusSeconds((long) i * 15 * 60), 5.0 + i,
                        Messwert.Qualitaet.GEMESSEN, Messwert.MesswertTyp.LASTGANG_15MIN));
            }

            // Hub aggregiert für Abrechnung
            AggregierterLastgang lastgang = hub.lastgangAbrufen(
                    BK, STICHTAG, STICHTAG.plusDays(1), NETZGEBIET);

            assertThat(hub.status().gespeicherteMesswerte()).isEqualTo(8);
            assertThat(lastgang.bilanzkreisId()).isEqualTo(BK);
            assertThat(lastgang.anzahlMalos()).isEqualTo(2);
            assertThat(lastgang.gesamtverbrauchKwh()).isGreaterThan(0);
        }
    }
}
