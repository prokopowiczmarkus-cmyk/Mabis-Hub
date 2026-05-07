package de.mabis.hub.api.controller;

import de.mabis.hub.api.MaBiSHubFassade;
import de.mabis.hub.api.dto.MesswertRequest;
import de.mabis.hub.audit.AuditEreignis;
import de.mabis.hub.audit.Auditiert;
import de.mabis.hub.domain.MaLoId;
import de.mabis.hub.domain.Messwert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messwerte")
@Tag(name = "MSB – Messwerteinlieferung", description = "Schnittstelle für Messstellenbetreiber")
public class MesswertController {

    private final MaBiSHubFassade hub;

    public MesswertController(MaBiSHubFassade hub) {
        this.hub = hub;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Auditiert(ereignis = AuditEreignis.MESSWERT_EINGELIEFERT, maloIdFeld = "maloId")
    @Operation(summary = "Messwert einliefern",
               description = "MSB liefert einen Messwert für eine Marktlokation ein. "
                           + "Stammdaten der MaLo-ID müssen zuvor vom VNB gemeldet worden sein.")
    public void messwertEinliefern(@Valid @RequestBody MesswertRequest req) {
        hub.messwertEinliefern(new Messwert(
                MaLoId.of(req.maloId()),
                req.zeitpunkt(),
                req.wertKwh(),
                Messwert.Qualitaet.valueOf(req.qualitaet()),
                Messwert.MesswertTyp.valueOf(req.typ())
        ));
    }
}
