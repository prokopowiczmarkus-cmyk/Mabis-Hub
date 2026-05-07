package de.mabis.hub.persistence.repository;

import de.mabis.hub.persistence.entity.AbrechnungEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbrechnungRepository extends JpaRepository<AbrechnungEntity, String> {

    List<AbrechnungEntity> findByBilanzkreisIdOrderByErstelltAmDesc(String bilanzkreisId);
}
