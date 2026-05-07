package de.mabis.hub.persistence.repository;

import de.mabis.hub.persistence.entity.FahrplanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FahrplanRepository extends JpaRepository<FahrplanEntity, UUID> {

    Optional<FahrplanEntity> findByBilanzkreisIdAndLiefertag(
            String bilanzkreisId, LocalDate liefertag);

    @Query("""
        SELECT f.liefertag FROM FahrplanEntity f
        WHERE f.bilanzkreisId = :bilanzkreisId
          AND f.liefertag BETWEEN :von AND :bis
        """)
    List<LocalDate> findVorhandeneTage(
            @Param("bilanzkreisId") String bilanzkreisId,
            @Param("von") LocalDate von,
            @Param("bis") LocalDate bis);
}
