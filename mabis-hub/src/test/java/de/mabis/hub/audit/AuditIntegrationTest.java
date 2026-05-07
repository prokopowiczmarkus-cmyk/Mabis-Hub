package de.mabis.hub.audit;

import de.mabis.hub.MaBiSHubApplication;
import de.mabis.hub.TestContainersBase;
import de.mabis.hub.api.dto.MesswertRequest;
import de.mabis.hub.api.dto.StammdatenRequest;
import de.mabis.hub.domain.MaLoId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = MaBiSHubApplication.class)
@AutoConfigureMockMvc
@DisplayName("Audit-Log – Integrationstests")
class AuditIntegrationTest extends TestContainersBase {

    @Autowired MockMvc      mvc;
    @Autowired AuditService auditService;
    @Autowired AuditRepository auditRepository;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final String MALO = MaLoId.random().value();
    private final String BK   = "11YSWKW-----------W";

    @BeforeEach
    void cleanAuditLog() {
        auditRepository.deleteAll();
    }

    @Test
    @DisplayName("Erfolgreiche Stammdatenmeldung erzeugt Audit-Eintrag")
    void stammdaten_erzeugen_audit_eintrag() throws Exception {
        var req = new StammdatenRequest(MALO, "NW-West", "RLM", BK,
                LocalDate.of(2026, 1, 1), null);

        mvc.perform(post("/api/v1/stammdaten")
                        .with(alsVnb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // @Async – kurz warten bis der Eintrag geschrieben ist
        await().atMost(3, SECONDS).untilAsserted(() -> {
            var seite = auditService.alleEintraege(0, 10);
            assertThat(seite.getTotalElements()).isEqualTo(1);
            AuditEintrag eintrag = seite.getContent().get(0);
            assertThat(eintrag.ereignis()).isEqualTo(AuditEreignis.STAMMDATEN_GEMELDET);
            assertThat(eintrag.maloId()).isEqualTo(MALO);
            assertThat(eintrag.bilanzkreisId()).isEqualTo(BK);
            assertThat(eintrag.marktRolle()).isEqualTo("VNB");
            assertThat(eintrag.erfolg()).isTrue();
        });
    }

    @Test
    @DisplayName("Fehlgeschlagener Messwert (unbekannte MaLo) erzeugt Fehler-Audit-Eintrag")
    void fehlgeschlagene_operation_wird_protokolliert() throws Exception {
        var req = new MesswertRequest(MaLoId.random().value(), Instant.now(),
                10.0, "GEMESSEN", "LASTGANG_15MIN");

        mvc.perform(post("/api/v1/messwerte")
                        .with(alsMsb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());

        await().atMost(3, SECONDS).untilAsserted(() -> {
            var fehler = auditService.letzteFehlschlaege(10);
            assertThat(fehler).hasSize(1);
            assertThat(fehler.get(0).ereignis()).isEqualTo(AuditEreignis.MESSWERT_EINGELIEFERT);
            assertThat(fehler.get(0).erfolg()).isFalse();
            assertThat(fehler.get(0).fehlerMeldung()).contains("Stammdaten zuerst übermitteln");
        });
    }

    @Test
    @DisplayName("Audit-Statistik zählt Erfolge und Misserfolge korrekt")
    void statistik_korrekt() throws Exception {
        // 1 erfolgreicher Aufruf
        var stammdaten = new StammdatenRequest(MALO, "NW", "SLP", BK,
                LocalDate.of(2026, 1, 1), null);
        mvc.perform(post("/api/v1/stammdaten")
                        .with(alsVnb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(stammdaten)))
                .andExpect(status().isCreated());

        // 1 fehlgeschlagener Aufruf
        var messwertFalsch = new MesswertRequest(MaLoId.random().value(), Instant.now(),
                5.0, "GEMESSEN", "LASTGANG_15MIN");
        mvc.perform(post("/api/v1/messwerte")
                        .with(alsMsb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(messwertFalsch)))
                .andExpect(status().isUnprocessableEntity());

        await().atMost(3, SECONDS).untilAsserted(() -> {
            AuditService.AuditStatistik statistik = auditService.statistik();
            assertThat(statistik.gesamt()).isEqualTo(2);
            assertThat(statistik.erfolgreich()).isEqualTo(1);
            assertThat(statistik.fehlgeschlagen()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("GET /audit ohne ADMIN-Rolle → 403 Forbidden")
    void audit_ohne_admin_verboten() throws Exception {
        mvc.perform(get("/api/v1/audit")
                        .with(alsVnb()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /audit mit ADMIN-Rolle → 200 mit leerem Log")
    void audit_mit_admin_erlaubt() throws Exception {
        mvc.perform(get("/api/v1/audit")
                        .with(alsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
