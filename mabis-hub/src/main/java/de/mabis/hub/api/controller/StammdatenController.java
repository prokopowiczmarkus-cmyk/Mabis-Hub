package de.mabis.hub.api.controller;

import de.mabis.hub.api.MaBiSHubFassade;
import de.mabis.hub.api.dto.StammdatenRequest;
import de.mabis.hub.audit.AuditEreignis;
import de.mabis.hub.audit.Auditiert;
import de.mabis.hub.domain.MaLoId;
import de.mabis.hub.domain.Stammdaten;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stammdaten")
@Tag(name = "VNB – Stammdatenmeldung", description = "Schnittstelle für Verteilnetzbetreiber")
public class StammdatenController {

    private final MaBiSHubFassade hub;

    public StammdatenController(MaBiSHubFassade hub) {
        this.hub = hub;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Auditiert(ereignis = AuditEreignis.STAMMDATEN_GEMELDET, maloIdFeld = "maloId",
               bilanzkreisIdFeld = "bilanzkreisId")
    @Operation(summary = "Stammdaten melden",
               description = "VNB meldet bilanzierungsrelevante Stammdaten einer Marktlokation. "
                           + "Muss vor der ersten Messwerteinlieferung durch den MSB erfolgen.")
    public void stammdatenMelden(@Valid @RequestBody StammdatenRequest req) {
        hub.stammdatenMelden(new Stammdaten(
                MaLoId.of(req.maloId()),
                req.netzgebiet(),
                Stammdaten.Bilanzierungsverfahren.valueOf(req.verfahren()),
                req.bilanzkreisId(),
                req.gueltigAb(),
                req.gueltigBis()
        ));
    }
}
