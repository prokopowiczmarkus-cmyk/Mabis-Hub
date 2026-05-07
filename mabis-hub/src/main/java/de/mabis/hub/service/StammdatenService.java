package de.mabis.hub.service;

import de.mabis.hub.domain.MaLoId;
import de.mabis.hub.domain.Stammdaten;
import de.mabis.hub.persistence.entity.StammdatenEntity;
import de.mabis.hub.persistence.repository.StammdatenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class StammdatenService {

    private static final Logger LOG = Logger.getLogger(StammdatenService.class.getName());

    private final StammdatenRepository repository;

    public StammdatenService(StammdatenRepository repository) {
        this.repository = repository;
    }

    public void stammdatenEmpfangen(Stammdaten stammdaten) {
        repository.save(StammdatenEntity.vonDomain(stammdaten));
        LOG.info("Stammdaten gespeichert | MaLo: " + stammdaten.maloId()
                + " | Bilanzkreis: " + stammdaten.bilanzkreisId()
                + " | ab: " + stammdaten.gueltigAb());
    }

    @Transactional(readOnly = true)
    public boolean istBekannt(MaLoId maloId) {
        return repository.existsByMaloId(maloId.value());
    }

    @Transactional(readOnly = true)
    public Optional<Stammdaten> stammdatenZumStichtag(MaLoId maloId, LocalDate stichtag) {
        return repository.findAktivByMaloIdAndStichtag(maloId.value(), stichtag)
                .map(StammdatenEntity::zuDomain);
    }

    @Transactional(readOnly = true)
    public List<MaLoId> malosImBilanzkreis(String bilanzkreisId, LocalDate stichtag) {
        return repository.findAlleAktivImBilanzkreis(bilanzkreisId, stichtag)
                .stream()
                .map(e -> MaLoId.of(e.getMaloId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public int gesamtanzahlMalos() {
        return (int) repository.count();
    }
}
