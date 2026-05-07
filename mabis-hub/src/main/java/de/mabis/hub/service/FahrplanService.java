package de.mabis.hub.service;

import de.mabis.hub.domain.Fahrplan;
import de.mabis.hub.persistence.entity.FahrplanEntity;
import de.mabis.hub.persistence.repository.FahrplanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
public class FahrplanService {

    private static final Logger LOG = Logger.getLogger(FahrplanService.class.getName());

    private final FahrplanRepository repository;

    public FahrplanService(FahrplanRepository repository) {
        this.repository = repository;
    }

    public void fahrplanEinreichen(Fahrplan fahrplan) {
        // Upsert: bestehenden Eintrag überschreiben (UNIQUE auf bilanzkreis_id + liefertag)
        repository.findByBilanzkreisIdAndLiefertag(fahrplan.bilanzkreisId(), fahrplan.liefertag())
                .ifPresent(repository::delete);

        repository.save(FahrplanEntity.vonDomain(fahrplan));
        LOG.info("Fahrplan gespeichert | BK: " + fahrplan.bilanzkreisId()
                + " | Tag: " + fahrplan.liefertag()
                + " | Gesamt: " + String.format("%.1f", fahrplan.gesamtKwh()) + " kWh");
    }

    @Transactional(readOnly = true)
    public Optional<Fahrplan> fahrplanLaden(String bilanzkreisId, LocalDate tag) {
        return repository.findByBilanzkreisIdAndLiefertag(bilanzkreisId, tag)
                .map(FahrplanEntity::zuDomain);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> fehlendeFahrplanTage(String bilanzkreisId,
                                                  LocalDate von, LocalDate bis) {
        Set<LocalDate> vorhanden = Set.copyOf(
                repository.findVorhandeneTage(bilanzkreisId, von, bis));

        List<LocalDate> fehlend = new ArrayList<>();
        for (LocalDate tag = von; !tag.isAfter(bis); tag = tag.plusDays(1)) {
            if (!vorhanden.contains(tag)) fehlend.add(tag);
        }
        return fehlend;
    }
}
