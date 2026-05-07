package de.mabis.hub.api;

import de.mabis.hub.MaBiSHubApplication;
import de.mabis.hub.TestContainersBase;
import de.mabis.hub.api.dto.*;
import de.mabis.hub.domain.MaLoId;
import de.mabis.hub.persistence.repository.MesswertRepository;
import de.mabis.hub.persistence.repository.StammdatenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.time.YearMonth;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = MaBiSHubApplication.class)
@AutoConfigureMockMvc
@DisplayName("REST-API Integrationstests")
class AbrechnungControllerTest extends TestContainersBase {

    @Autowired MockMvc mvc;
    @Autowired StammdatenRepository stammdatenRepo;
    @Autowired MesswertRepository   messwertRepo;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final String MALO = MaLoId.random().value();
    private final String BK   = "11YSWKW-----------W";

    @BeforeEach
    void cleanDb() {
        messwertRepo.deleteAll();
        stammdatenRepo.deleteAll();
    }

    // ── Stammdaten ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("VNB: POST /stammdaten → 201 Created")
    void stammdaten_melden_201() throws Exception {
        var req = new StammdatenRequest(MALO, "NW-West", "RLM", BK,
                LocalDate.of(2026, 1, 1), null);

        mvc.perform(post("/api/v1/stammdaten")
                        .with(alsVnb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("MSB: POST /stammdaten → 403 Forbidden (falsche Rolle)")
    void stammdaten_falsche_rolle_403() throws Exception {
        var req = new StammdatenRequest(MALO, "NW-West", "RLM", BK,
                LocalDate.of(2026, 1, 1), null);

        mvc.perform(post("/api/v1/stammdaten")
                        .with(alsMsb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ohne Token: POST /stammdaten → 401 Unauthorized")
    void stammdaten_ohne_token_401() throws Exception {
        var req = new StammdatenRequest(MALO, "NW-West", "RLM", BK,
                LocalDate.of(2026, 1, 1), null);

        mvc.perform(post("/api/v1/stammdaten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── Messwerte ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MSB: POST /messwerte ohne Stammdaten → 422 (Zugriff OK, Regel verletzt)")
    void messwert_ohne_stammdaten_422() throws Exception {
        var req = new MesswertRequest(MaLoId.random().value(), Instant.now(),
                10.0, "GEMESSEN", "LASTGANG_15MIN");

        mvc.perform(post("/api/v1/messwerte")
                        .with(alsMsb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("VNB: POST /messwerte → 403 Forbidden")
    void messwert_vnb_403() throws Exception {
        var req = new MesswertRequest(MALO, Instant.now(), 10.0, "GEMESSEN", "LASTGANG_15MIN");

        mvc.perform(post("/api/v1/messwerte")
                        .with(alsVnb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── Fahrplan ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BKV: POST /fahrplaene mit 95 Werten → 400 Validierungsfehler")
    void fahrplan_falsche_anzahl_400() throws Exception {
        var req = new FahrplanRequest(BK, LocalDate.of(2026, 1, 1),
                Collections.nCopies(95, 10.0), "ENTNAHME", "BKV-001");

        mvc.perform(post("/api/v1/fahrplaene")
                        .with(alsBkv())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validierungsfehler"));
    }

    @Test
    @DisplayName("MSB: POST /fahrplaene → 403 Forbidden")
    void fahrplan_msb_403() throws Exception {
        var req = new FahrplanRequest(BK, LocalDate.of(2026, 1, 1),
                Collections.nCopies(96, 10.0), "ENTNAHME", "BKV-001");

        mvc.perform(post("/api/v1/fahrplaene")
                        .with(alsMsb())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── Abrechnungen ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("BKV: GET /abrechnungen/{id} unbekannte ID → 404")
    void abrechnung_unbekannte_id_404() throws Exception {
        mvc.perform(get("/api/v1/abrechnungen/UNBEKANNT")
                        .with(alsBkv()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MSB: GET /abrechnungen → 403 Forbidden")
    void abrechnungen_msb_403() throws Exception {
        mvc.perform(get("/api/v1/abrechnungen")
                        .with(alsMsb())
                        .param("bilanzkreisId", BK))
                .andExpect(status().isForbidden());
    }

    // ── Status & Audit ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /abrechnungen/status → 200 für alle authentifizierten Rollen")
    void hub_status_200() throws Exception {
        mvc.perform(get("/api/v1/abrechnungen/status")
                        .with(alsMsb()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrierteMalos").isNumber())
                .andExpect(jsonPath("$.gespeicherteMesswerte").isNumber());
    }

    @Test
    @DisplayName("GET /audit ohne Token → 401 Unauthorized")
    void audit_ohne_token_401() throws Exception {
        mvc.perform(get("/api/v1/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /audit mit VNB-Rolle → 403 Forbidden")
    void audit_vnb_403() throws Exception {
        mvc.perform(get("/api/v1/audit")
                        .with(alsVnb()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /audit mit ADMIN-Rolle → 200 OK")
    void audit_admin_200() throws Exception {
        mvc.perform(get("/api/v1/audit")
                        .with(alsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
