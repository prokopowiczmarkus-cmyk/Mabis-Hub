-- Audit-Log: append-only, keine UPDATE/DELETE-Rechte für Applikationsuser
-- Regulatorische Anforderung: lückenlose Nachvollziehbarkeit aller Hub-Operationen

CREATE TABLE audit_log (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    ereignis        VARCHAR(60)  NOT NULL,
    markt_rolle     VARCHAR(20),                   -- Rolle aus JWT (MSB, VNB, BKV, UNB)
    principal_id    VARCHAR(150),                  -- JWT sub-Claim (Nutzer-ID im IdP)
    malo_id         VARCHAR(33),                   -- betroffene MaLo-ID (falls vorhanden)
    bilanzkreis_id  VARCHAR(50),                   -- betroffener Bilanzkreis (falls vorhanden)
    details         TEXT,                          -- JSON mit weiteren Kontextdaten
    erfolg          BOOLEAN      NOT NULL,
    fehler_meldung  TEXT,                          -- bei Misserfolg
    zeitpunkt       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    anfrage_ip      VARCHAR(50)
);

-- Indizes für regulatorische Abfragen (wer hat wann was getan?)
CREATE INDEX idx_audit_zeitpunkt      ON audit_log (zeitpunkt DESC);
CREATE INDEX idx_audit_principal      ON audit_log (principal_id, zeitpunkt DESC);
CREATE INDEX idx_audit_malo           ON audit_log (malo_id)         WHERE malo_id IS NOT NULL;
CREATE INDEX idx_audit_bilanzkreis    ON audit_log (bilanzkreis_id)  WHERE bilanzkreis_id IS NOT NULL;
CREATE INDEX idx_audit_ereignis       ON audit_log (ereignis, zeitpunkt DESC);

-- Kommentar zur Datenhaltungspflicht
COMMENT ON TABLE audit_log IS
    'Unveränderliches Audit-Log aller MaBiS-Hub-Operationen. '
    'Aufbewahrungspflicht gemäß §257 HGB: 10 Jahre.';
