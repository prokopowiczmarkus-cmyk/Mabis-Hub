package de.mabis.hub.api.controller;

import de.mabis.hub.api.MaBiSHubFassade;
import de.mabis.hub.api.dto.FahrplanRequest;
import de.mabis.hub.audit.AuditEreignis;
import de.mabis.hub.audit.Auditiert;
import de.mabis.hub.domain.Fahrplan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fahrplaene")
@Tag(name = "BKV – Fahrplaneinreichung", description = "Schnittstelle für Bilanzkreisverantwortliche")
public class FahrplanController {

    private final MaBiSHubFassade hub;

    public FahrplanController(MaBiSHubFassade hub) {
        this.hub = hub;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Auditiert(ereignis = AuditEreignis.FAHRPLAN_EINGEREICHT, bilanzkreisIdFeld = "bilanzkreisId")
    @Operation(summary = "Tagesfahrplan einreichen",
               description = "BKV reicht 96 Viertelstundenwerte (kWh) für einen Liefertag ein. "
                           + "Ein bereits eingereichter Fahrplan wird überschrieben.")
    public void fahrplanEinreichen(@Valid @RequestBody FahrplanRequest req) {
        hub.fahrplanEinreichen(new Fahrplan(
                req.bilanzkreisId(),
                req.liefertag(),
                req.viertelstundenwerteKwh(),
                Fahrplan.FahrplanTyp.valueOf(req.typ()),
                req.bkvId()
        ));
    }
}
