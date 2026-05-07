package de.mabis.hub.api.controller;

import de.mabis.hub.api.MaBiSHubFassade;
import de.mabis.hub.api.dto.AbrechnungResponse;
import de.mabis.hub.api.dto.AbrechnungslaufRequest;
import de.mabis.hub.audit.AuditEreignis;
import de.mabis.hub.audit.Auditiert;
import de.mabis.hub.domain.Abrechnungsperiode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/abrechnungen")
@Tag(name = "Bilanzkreisabrechnung", description = "Abrechnungsläufe und Ergebnisabruf")
public class AbrechnungController {

    private final MaBiSHubFassade hub;

    public AbrechnungController(MaBiSHubFassade hub) {
        this.hub = hub;
    }

    @PostMapping("/laeufe")
    @ResponseStatus(HttpStatus.CREATED)
    @Auditiert(ereignis = AuditEreignis.ABRECHNUNGSLAUF_GESTARTET, bilanzkreisIdFeld = "bilanzkreisId")
    @Operation(summary = "Abrechnungslauf anstoßen",
               description = "Startet einen Abrechnungslauf. Lauf: VORLAEUFIG | KORRIGIERT | ENDGUELTIG.")
    public AbrechnungResponse laufAnstoßen(@Valid @RequestBody AbrechnungslaufRequest req) {
        var abrechnung = hub.abrechnungslaufAnstoßen(
                req.bilanzkreisId(), req.yearMonth(),
                Abrechnungsperiode.Lauf.valueOf(req.lauf()),
                req.netzgebiet(), req.aePreisEurKwh());
        return AbrechnungResponse.von(abrechnung);
    }

    @GetMapping("/{abrechnungsId}")
    @Auditiert(ereignis = AuditEreignis.ABRECHNUNG_ABGERUFEN)
    @Operation(summary = "Abrechnung abrufen")
    public AbrechnungResponse abrechnungAbrufen(
            @Parameter(description = "Eindeutige Abrechnungs-ID") @PathVariable String abrechnungsId) {
        return AbrechnungResponse.von(hub.abrechnungAbrufen(abrechnungsId));
    }

    @GetMapping
    @Operation(summary = "Alle Abrechnungen eines Bilanzkreises abrufen")
    public List<AbrechnungResponse> alleAbrechnungen(
            @Parameter(description = "Bilanzkreis-ID") @RequestParam String bilanzkreisId) {
        return hub.alleAbrechnungen(bilanzkreisId).stream().map(AbrechnungResponse::von).toList();
    }

    @GetMapping("/status")
    @Operation(summary = "Hub-Status abrufen")
    public MaBiSHubFassade.HubStatus hubStatus() {
        return hub.status();
    }
}
