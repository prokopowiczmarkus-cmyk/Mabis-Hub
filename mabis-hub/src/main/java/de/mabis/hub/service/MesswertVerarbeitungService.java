package de.mabis.hub.service;

import de.mabis.hub.domain.MaLoId;
import de.mabis.hub.domain.Messwert;
import de.mabis.hub.persistence.entity.MesswertEntity;
import de.mabis.hub.persistence.repository.MesswertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
@Transactional
public class MesswertVerarbeitungService {

    private static final Logger LOG = Logger.getLogger(MesswertVerarbeitungService.class.getName());

    private final MesswertRepository repository;
    private final StammdatenService stammdatenService;

    public MesswertVerarbeitungService(MesswertRepository repository,
                                       StammdatenService stammdatenService) {
        this.repository       = repository;
        this.stammdatenService = stammdatenService;
    }

    public void messwertEmpfangen(Messwert messwert) {
        if (!stammdatenService.istBekannt(messwert.maloId())) {
            throw new IllegalStateException(
                "MaLo-ID unbekannt – VNB muss Stammdaten zuerst übermitteln: " + messwert.maloId());
        }
        if (messwert.istErsatzwert()) {
            LOG.warning("Ersatzwert empfangen für MaLo: " + messwert.maloId()
                    + " – Zeitpunkt: " + messwert.zeitpunkt());
        }
        repository.save(MesswertEntity.vonDomain(messwert));
        LOG.info("Messwert gespeichert | MaLo: " + messwert.maloId()
                + " | " + messwert.wertKwh() + " kWh");
    }

    @Transactional(readOnly = true)
    public List<Messwert> messwerteLaden(MaLoId maloId, Instant von, Instant bis) {
        return repository.findByMaloIdAndZeitraum(maloId.value(), von, bis)
                .stream()
                .map(MesswertEntity::zuDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean istVollstaendig(MaLoId maloId, Instant von, Instant bis) {
        long erwartet = (bis.getEpochSecond() - von.getEpochSecond()) / (15 * 60);
        long vorhanden = repository.countByMaloIdAndZeitraum(maloId.value(), von, bis);
        return vorhanden >= erwartet;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> anzahlMesswerteJeMalo() {
        return Map.of("gesamt", repository.count());
    }
}
