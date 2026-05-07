package de.mabis.hub.security;

import de.mabis.hub.MaBiSHubApplication;
import de.mabis.hub.TestContainersBase;
import de.mabis.hub.api.dto.*;
import de.mabis.hub.domain.MaLoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rollenbasierte Zugriffstests für alle MaBiS-Hub-Endpunkte.
 */
@SpringBootTest(classes = MaBiSHubApplication.class)
@AutoConfigureMockMvc
@DisplayName("Security – Rollenbasierte Zugriffskontrolle")
class SecurityTest extends TestContainersBase {

    @Autowired MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ── Hilfsmethoden für JWT-Tokens je Marktrolle ───────────────────────────

    private static org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor alsRolle(String rolle) {
        return jwt().authorities(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + rolle)
        );
    }

    // ── POST /api/v1/stammdaten (VNB) ────────────────────────────────────────

    @Nested @DisplayName("Stammdaten-Endpunkt (VNB)")
    class StammdatenEndpunkt {

        @Test @DisplayName("VNB → 201 Created")
        void vnb_darf_stammdaten_melden() throws Exception {
            mvc.perform(post("/api/v1/stammdaten")
                            .with(alsRolle("VNB"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(stammdatenJson()))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("MSB → 403 Forbidden")
        void msb_darf_keine_stammdaten_melden() throws Exception {
            mvc.perform(post("/api/v1/stammdaten")
                            .with(alsRolle("MSB"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(stammdatenJson()))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("ADMIN → 201 (Vollzugriff)")
        void admin_darf_stammdaten_melden() throws Exception {
            mvc.perform(post("/api/v1/stammdaten")
                            .with(alsRolle("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(stammdatenJson()))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("Ohne Token → 401 Unauthorized")
        void ohne_token_401() throws Exception {
            mvc.perform(post("/api/v1/stammdaten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(stammdatenJson()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── POST /api/v1/messwerte (MSB) ─────────────────────────────────────────

    @Nested @DisplayName("Messwert-Endpunkt (MSB)")
    class MesswertEndpunkt {

        @Test @DisplayName("MSB → 422 (Stammdaten fehlen – aber Zugriff erlaubt)")
        void msb_hat_zugriff_auf_messwerte() throws Exception {
            mvc.perform(post("/api/v1/messwerte")
                            .with(alsRolle("MSB"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(messwertJson()))
                    .andExpect(status().isUnprocessableEntity()); // Zugriff OK, Stammdaten fehlen
        }

        @Test @DisplayName("VNB → 403 Forbidden")
        void vnb_darf_keine_messwerte_einliefern() throws Exception {
            mvc.perform(post("/api/v1/messwerte")
                            .with(alsRolle("VNB"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(messwertJson()))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("BKV → 403 Forbidden")
        void bkv_darf_keine_messwerte_einliefern() throws Exception {
            mvc.perform(post("/api/v1/messwerte")
                            .with(alsRolle("BKV"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(messwertJson()))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("Ohne Token → 401 Unauthorized")
        void ohne_token_401() throws Exception {
            mvc.perform(post("/api/v1/messwerte")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(messwertJson()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── POST /api/v1/fahrplaene (BKV) ────────────────────────────────────────

    @Nested @DisplayName("Fahrplan-Endpunkt (BKV)")
    class FahrplanEndpunkt {

        @Test @DisplayName("BKV → 201 Created")
        void bkv_darf_fahrplan_einreichen() throws Exception {
            mvc.perform(post("/api/v1/fahrplaene")
                            .with(alsRolle("BKV"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fahrplanJson()))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("MSB → 403 Forbidden")
        void msb_darf_keinen_fahrplan_einreichen() throws Exception {
            mvc.perform(post("/api/v1/fahrplaene")
                            .with(alsRolle("MSB"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fahrplanJson()))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("VNB → 403 Forbidden")
        void vnb_darf_keinen_fahrplan_einreichen() throws Exception {
            mvc.perform(post("/api/v1/fahrplaene")
                            .with(alsRolle("VNB"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fahrplanJson()))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("Ohne Token → 401 Unauthorized")
        void ohne_token_401() throws Exception {
            mvc.perform(post("/api/v1/fahrplaene")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fahrplanJson()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── POST /api/v1/abrechnungen/laeufe (ÜNB) ───────────────────────────────

    @Nested @DisplayName("Abrechnungslauf-Endpunkt (ÜNB)")
    class AbrechnungslaufEndpunkt {

        @Test @DisplayName("UNB → 201 Created")
        void unb_darf_lauf_anstoessen() throws Exception {
            mvc.perform(post("/api/v1/abrechnungen/laeufe")
                            .with(alsRolle("UNB"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(abrechnungslaufJson()))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("BKV → 403 Forbidden (kein Recht auf Hub-interne Operationen)")
        void bkv_darf_keinen_lauf_anstoessen() throws Exception {
            mvc.perform(post("/api/v1/abrechnungen/laeufe")
                            .with(alsRolle("BKV"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(abrechnungslaufJson()))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("MSB → 403 Forbidden")
        void msb_darf_keinen_lauf_anstoessen() throws Exception {
            mvc.perform(post("/api/v1/abrechnungen/laeufe")
                            .with(alsRolle("MSB"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(abrechnungslaufJson()))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("Ohne Token → 401 Unauthorized")
        void ohne_token_401() throws Exception {
            mvc.perform(post("/api/v1/abrechnungen/laeufe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(abrechnungslaufJson()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/v1/abrechnungen (BKV + ÜNB) ────────────────────────────────

    @Nested @DisplayName("Abrechnungsabruf (BKV + ÜNB)")
    class AbrechnungsabrufEndpunkt {

        @Test @DisplayName("BKV → 200 OK (leere Liste)")
        void bkv_darf_abrechnungen_abrufen() throws Exception {
            mvc.perform(get("/api/v1/abrechnungen")
                            .with(alsRolle("BKV"))
                            .param("bilanzkreisId", "11YSWKW-----------W"))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("UNB → 200 OK")
        void unb_darf_abrechnungen_abrufen() throws Exception {
            mvc.perform(get("/api/v1/abrechnungen")
                            .with(alsRolle("UNB"))
                            .param("bilanzkreisId", "11YSWKW-----------W"))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("MSB → 403 Forbidden (keine Einsicht in Abrechnungen)")
        void msb_darf_keine_abrechnungen_sehen() throws Exception {
            mvc.perform(get("/api/v1/abrechnungen")
                            .with(alsRolle("MSB"))
                            .param("bilanzkreisId", "11YSWKW-----------W"))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("Ohne Token → 401 Unauthorized")
        void ohne_token_401() throws Exception {
            mvc.perform(get("/api/v1/abrechnungen")
                            .param("bilanzkreisId", "11YSWKW-----------W"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Swagger – öffentlich zugänglich ──────────────────────────────────────

    @Nested @DisplayName("Öffentliche Endpunkte")
    class OeffentlicheEndpunkte {

        @Test @DisplayName("Swagger UI – kein Token erforderlich")
        void swagger_ist_oeffentlich() throws Exception {
            mvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().is3xxRedirection()); // Redirect zur swagger-ui/index.html
        }

        @Test @DisplayName("OpenAPI-Docs – kein Token erforderlich")
        void api_docs_sind_oeffentlich() throws Exception {
            mvc.perform(get("/api-docs"))
                    .andExpect(status().isOk());
        }
    }

    // ── Test-Daten ────────────────────────────────────────────────────────────

    private String stammdatenJson() throws Exception {
        return mapper.writeValueAsString(new StammdatenRequest(
                MaLoId.random().value(), "NW-West", "RLM",
                "11YSWKW-----------W", LocalDate.of(2026, 1, 1), null));
    }

    private String messwertJson() throws Exception {
        return mapper.writeValueAsString(new MesswertRequest(
                MaLoId.random().value(), Instant.now(), 10.0, "GEMESSEN", "LASTGANG_15MIN"));
    }

    private String fahrplanJson() throws Exception {
        return mapper.writeValueAsString(new FahrplanRequest(
                "11YSWKW-----------W", LocalDate.of(2026, 1, 1),
                Collections.nCopies(96, 100.0), "ENTNAHME", "BKV-001"));
    }

    private String abrechnungslaufJson() throws Exception {
        return mapper.writeValueAsString(new AbrechnungslaufRequest(
                "11YSWKW-----------W", YearMonth.of(2026, 1),
                "VORLAEUFIG", "NW-West", 0.12));
    }
}
