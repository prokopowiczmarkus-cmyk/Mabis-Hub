package de.mabis.hub.persistence.repository;

import de.mabis.hub.persistence.entity.MesswertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MesswertRepository extends JpaRepository<MesswertEntity, UUID> {

    @Query("""
        SELECT m FROM MesswertEntity m
        WHERE m.maloId = :maloId
          AND m.zeitpunkt >= :von
          AND m.zeitpunkt <= :bis
        ORDER BY m.zeitpunkt ASC
        """)
    List<MesswertEntity> findByMaloIdAndZeitraum(
            @Param("maloId") String maloId,
            @Param("von") Instant von,
            @Param("bis") Instant bis);

    @Query("""
        SELECT COUNT(m) FROM MesswertEntity m
        WHERE m.maloId = :maloId
          AND m.zeitpunkt >= :von
          AND m.zeitpunkt <= :bis
        """)
    long countByMaloIdAndZeitraum(
            @Param("maloId") String maloId,
            @Param("von") Instant von,
            @Param("bis") Instant bis);

    @Query("SELECT COUNT(DISTINCT m.maloId) FROM MesswertEntity m")
    long countDistinctMalos();

    long count();
}
