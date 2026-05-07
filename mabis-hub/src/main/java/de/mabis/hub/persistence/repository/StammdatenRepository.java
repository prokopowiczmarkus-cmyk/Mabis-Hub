package de.mabis.hub.persistence.repository;

import de.mabis.hub.persistence.entity.StammdatenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StammdatenRepository extends JpaRepository<StammdatenEntity, UUID> {

    boolean existsByMaloId(String maloId);

    @Query("""
        SELECT s FROM StammdatenEntity s
        WHERE s.maloId = :maloId
          AND s.gueltigAb <= :stichtag
          AND (s.gueltigBis IS NULL OR s.gueltigBis >= :stichtag)
        ORDER BY s.gueltigAb DESC
        LIMIT 1
        """)
    Optional<StammdatenEntity> findAktivByMaloIdAndStichtag(
            @Param("maloId") String maloId,
            @Param("stichtag") LocalDate stichtag);

    @Query("""
        SELECT s FROM StammdatenEntity s
        WHERE s.bilanzkreisId = :bilanzkreisId
          AND s.gueltigAb <= :stichtag
          AND (s.gueltigBis IS NULL OR s.gueltigBis >= :stichtag)
        """)
    List<StammdatenEntity> findAlleAktivImBilanzkreis(
            @Param("bilanzkreisId") String bilanzkreisId,
            @Param("stichtag") LocalDate stichtag);
}
