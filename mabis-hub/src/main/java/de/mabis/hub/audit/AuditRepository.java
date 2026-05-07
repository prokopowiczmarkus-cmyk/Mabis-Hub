package de.mabis.hub.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditRepository extends JpaRepository<AuditEntity, UUID> {

    Page<AuditEntity> findByOrderByZeitpunktDesc(Pageable pageable);

    Page<AuditEntity> findByPrincipalIdOrderByZeitpunktDesc(String principalId, Pageable pageable);

    Page<AuditEntity> findByMaloIdOrderByZeitpunktDesc(String maloId, Pageable pageable);

    Page<AuditEntity> findByBilanzkreisIdOrderByZeitpunktDesc(String bilanzkreisId, Pageable pageable);

    Page<AuditEntity> findByEreignisOrderByZeitpunktDesc(String ereignis, Pageable pageable);

    @Query("""
        SELECT a FROM AuditEntity a
        WHERE a.zeitpunkt >= :von AND a.zeitpunkt <= :bis
        ORDER BY a.zeitpunkt DESC
        """)
    Page<AuditEntity> findImZeitraum(
            @Param("von") Instant von,
            @Param("bis") Instant bis,
            Pageable pageable);

    @Query("""
        SELECT a FROM AuditEntity a
        WHERE a.erfolg = false
        ORDER BY a.zeitpunkt DESC
        """)
    List<AuditEntity> findFehlerEintraege(Pageable pageable);

    long countByErfolg(boolean erfolg);
}
