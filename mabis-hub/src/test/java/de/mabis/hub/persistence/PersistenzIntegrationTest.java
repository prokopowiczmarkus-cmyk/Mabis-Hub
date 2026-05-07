package de.mabis.hub.persistence;

import de.mabis.hub.MaBiSHubApplication;
import de.mabis.hub.TestContainersBase;
import de.mabis.hub.domain.*;
import de.mabis.hub.persistence.entity.*;
import de.mabis.hub.persistence.repository.*;
import de.mabis.hub.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = MaBiSHubApplication.class)
@DisplayName("Persistenz – Testcontainers (PostgreSQL)")
class PersistenzIntegrationTest extends TestContainersBase {

    @Autowired StammdatenService   stammdatenService;
    @Autowired StammdatenRepository stammdatenRepo;
    @Autowired MesswertRepository   messwertRepo;
    @Autowired FahrplanRepository   fahrplanRepo;
    @Autowired AbrechnungRepository abrechnungRepo;

    private static final String BK = "11YSWKW-----------W";
    private static final LocalDate STICHTAG = LocalDate.of(2026, 1, 1);

    @BeforeEach
    void cleanDb() {
        abrechnungRepo.deleteAll();
        fahrplanRepo.deleteAll();
        messwertRepo.deleteAll();
        stammdatenRepo.deleteAll();
    }

    // ── Stammdaten ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Stammdaten speichern und per MaLo-ID finden")
    void stammdaten_persist_and_find() {
        MaLoId malo = MaLoId.random();
        stammdatenService.stammdatenEmpfangen(new Stammdaten(
                malo, "NW-West", Stammdaten.Bilanzierungsverfahren.RLM,
                BK, STICHTAG, null));

        assertThat(stammdatenService.istBekannt(malo)).isTrue();
        assertThat(stammdatenService.stammdatenZumStichtag(malo, STICHTAG))
                .isPresent()
                .hasValueSatisfying(s -> {
                    assertThat(s.maloId()).isEqualTo(malo);
                    assertThat(s.verfahren()).isEqualTo(Stammdaten.Bilanzierungsverfahren.RLM);
                    assertThat(s.bilanzkreisId()).isEqualTo(BK);
                });
    }

    @Test
    @DisplayName("Alle MaLos eines Bilanzkreises zum Stichtag finden")
    void malos_im_bilanzkreis() {
        MaLoId m1 = MaLoId.random();
        MaLoId m2 = MaLoId.random();
        stammdatenService.stammdatenEmpfangen(
                new Stammdaten(m1, "NW", Stammdaten.Bilanzierungsverfahren.SLP, BK, STICHTAG, null));
        stammdatenService.stammdatenEmpfangen(
                new Stammdaten(m2, "NW", Stammdaten.Bilanzierungsverfahren.RLM, BK, STICHTAG, null));

        List<MaLoId> malos = stammdatenService.malosImBilanzkreis(BK, STICHTAG);
        assertThat(malos).hasSize(2).containsExactlyInAnyOrder(m1, m2);
    }

    @Test
    @DisplayName("Stammdaten außerhalb Gültigkeitszeitraum werden nicht gefunden")
    void stammdaten_ausserhalb_gueltigkeit() {
        MaLoId malo = MaLoId.random();
        stammdatenService.stammdatenEmpfangen(new Stammdaten(
                malo, "NW", Stammdaten.Bilanzierungsverfahren.SLP,
                BK, STICHTAG, STICHTAG.plusMonths(6)));

        // Vor Gültigkeitsbeginn
        assertThat(stammdatenService.stammdatenZumStichtag(malo, STICHTAG.minusDays(1))).isEmpty();
        // Nach Gültigkeitsende
        assertThat(stammdatenService.stammdatenZumStichtag(malo, STICHTAG.plusMonths(7))).isEmpty();
        // Innerhalb → vorhanden
        assertThat(stammdatenService.stammdatenZumStichtag(malo, STICHTAG.plusMonths(3))).isPresent();
    }

    // ── Messwerte ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Messwert speichern und im Zeitraum laden")
    void messwert_persist_and_find() {
        MaLoId malo = MaLoId.random();
        stammdatenService.stammdatenEmpfangen(new Stammdaten(
                malo, "NW", Stammdaten.Bilanzierungsverfahren.RLM, BK, STICHTAG, null));

        Instant basis = STICHTAG.atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant();
        MesswertEntity m = MesswertEntity.vonDomain(new Messwert(
                malo, basis, 42.5, Messwert.Qualitaet.GEMESSEN, Messwert.MesswertTyp.LASTGANG_15MIN));
        messwertRepo.save(m);

        List<MesswertEntity> gefunden = messwertRepo.findByMaloIdAndZeitraum(
                malo.value(), basis.minusSeconds(1), basis.plusSeconds(1));

        assertThat(gefunden).hasSize(1);
        assertThat(gefunden.get(0).getWertKwh()).isEqualTo(42.5);
    }

    // ── Fahrpläne ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Fahrplan speichern und per BK + Tag laden")
    void fahrplan_persist_and_find() {
        List<Double> werte = Collections.nCopies(96, 100.0);
        FahrplanEntity fp = FahrplanEntity.vonDomain(
                new Fahrplan(BK, STICHTAG, werte, Fahrplan.FahrplanTyp.ENTNAHME, "BKV-001"));
        fahrplanRepo.save(fp);

        Optional<FahrplanEntity> gefunden = fahrplanRepo.findByBilanzkreisIdAndLiefertag(BK, STICHTAG);
        assertThat(gefunden).isPresent();
        assertThat(gefunden.get().getViertelstundenwerteKwh()).hasSize(96);
        assertThat(gefunden.get().getViertelstundenwerteKwh().get(0)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Fehlende Fahrplantage korrekt identifizieren")
    void fehlende_fahrplan_tage() {
        fahrplanRepo.save(FahrplanEntity.vonDomain(new Fahrplan(
                BK, STICHTAG, Collections.nCopies(96, 50.0), Fahrplan.FahrplanTyp.ENTNAHME, "BKV")));

        List<LocalDate> vorhandene = fahrplanRepo.findVorhandeneTage(BK, STICHTAG, STICHTAG.plusDays(2));
        assertThat(vorhandene).containsExactly(STICHTAG);
        // STICHTAG+1 und +2 fehlen → würden als fehlend zurückgegeben
    }

    // ── Abrechnungen ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Abrechnung persistieren und per ID laden")
    void abrechnung_persist_and_find() {
        Abrechnungsperiode periode = new Abrechnungsperiode(
                YearMonth.of(2026, 1), Abrechnungsperiode.Lauf.VORLAEUFIG);
        AusgleichsEnergieSaldo saldo = new AusgleichsEnergieSaldo(
                BK, periode, List.of(10.0, -5.0, 3.0), 0.12);
        Abrechnung abrechnung = new Abrechnung(
                "TEST0001", BK, "BKV-001", periode,
                5000.0, 4800.0, saldo, saldo.ausgleichsenergiekostenEur(),
                Abrechnung.AbrechnungsStatus.ERSTELLT, Instant.now());

        abrechnungRepo.save(AbrechnungEntity.vonDomain(abrechnung));

        Optional<AbrechnungEntity> geladen = abrechnungRepo.findById("TEST0001");
        assertThat(geladen).isPresent();
        Abrechnung wiederhergestellt = geladen.get().zuDomain();

        assertThat(wiederhergestellt.abrechnungsId()).isEqualTo("TEST0001");
        assertThat(wiederhergestellt.istVerbrauchKwh()).isEqualTo(5000.0);
        assertThat(wiederhergestellt.abweichungKwh()).isEqualTo(200.0);
        assertThat(wiederhergestellt.ausgleichsEnergieSaldo().saldoJeIntervallKwh())
                .containsExactly(10.0, -5.0, 3.0);
    }

    @Test
    @DisplayName("Alle Abrechnungen eines Bilanzkreises finden")
    void alle_abrechnungen_eines_bilanzkreises() {
        for (Abrechnungsperiode.Lauf lauf : Abrechnungsperiode.Lauf.values()) {
            Abrechnungsperiode p = new Abrechnungsperiode(YearMonth.of(2026, 1), lauf);
            AusgleichsEnergieSaldo s = new AusgleichsEnergieSaldo(BK, p, List.of(0.0), 0.12);
            abrechnungRepo.save(AbrechnungEntity.vonDomain(new Abrechnung(
                    lauf.name().substring(0, 8), BK, "BKV", p,
                    100.0, 100.0, s, 0.0,
                    Abrechnung.AbrechnungsStatus.ERSTELLT, Instant.now())));
        }

        List<AbrechnungEntity> alle = abrechnungRepo.findByBilanzkreisIdOrderByErstelltAmDesc(BK);
        assertThat(alle).hasSize(3);
    }
}
