-- MaBiS-Hub Datenbankschema
-- BK6-24-210: Persistenz bilanzierungsrelevanter Daten

CREATE TABLE stammdaten (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    malo_id         VARCHAR(33) NOT NULL,
    netzgebiet      VARCHAR(100) NOT NULL,
    verfahren       VARCHAR(10)  NOT NULL CHECK (verfahren IN ('SLP', 'RLM', 'iMSys')),
    bilanzkreis_id  VARCHAR(50)  NOT NULL,
    gueltig_ab      DATE         NOT NULL,
    gueltig_bis     DATE,
    erstellt_am     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_stammdaten_malo_id        ON stammdaten (malo_id);
CREATE INDEX idx_stammdaten_bilanzkreis_id ON stammdaten (bilanzkreis_id);
CREATE INDEX idx_stammdaten_gueltig        ON stammdaten (malo_id, gueltig_ab, gueltig_bis);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE messwerte (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    malo_id     VARCHAR(33) NOT NULL,
    zeitpunkt   TIMESTAMPTZ NOT NULL,
    wert_kwh    DOUBLE PRECISION NOT NULL CHECK (wert_kwh >= 0),
    qualitaet   VARCHAR(15)  NOT NULL CHECK (qualitaet IN ('GEMESSEN', 'ERSATZWERT', 'PROGNOSEWERT')),
    typ         VARCHAR(20)  NOT NULL CHECK (typ IN ('LASTGANG_15MIN', 'ZAEHLERSTAND', 'TAGESGANG')),
    erstellt_am TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (malo_id, zeitpunkt, typ)
);

CREATE INDEX idx_messwerte_malo_zeitpunkt ON messwerte (malo_id, zeitpunkt);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE fahrplaene (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    bilanzkreis_id          VARCHAR(50)  NOT NULL,
    liefertag               DATE         NOT NULL,
    viertelstundenwerte_json TEXT         NOT NULL,   -- JSON-Array mit 96 Werten
    typ                     VARCHAR(15)  NOT NULL CHECK (typ IN ('EINSPEISUNG', 'ENTNAHME')),
    bkv_id                  VARCHAR(50)  NOT NULL,
    eingereicht_am          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (bilanzkreis_id, liefertag)
);

CREATE INDEX idx_fahrplaene_bk_tag ON fahrplaene (bilanzkreis_id, liefertag);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE abrechnungen (
    abrechnungs_id          VARCHAR(20)      PRIMARY KEY,
    bilanzkreis_id          VARCHAR(50)      NOT NULL,
    bkv_id                  VARCHAR(50)      NOT NULL,
    periode_monat           VARCHAR(7)       NOT NULL,  -- Format: YYYY-MM
    periode_lauf            VARCHAR(15)      NOT NULL CHECK (periode_lauf IN ('VORLAEUFIG', 'KORRIGIERT', 'ENDGUELTIG')),
    ist_verbrauch_kwh       DOUBLE PRECISION NOT NULL,
    fahrplan_kwh            DOUBLE PRECISION NOT NULL,
    saldo_werte_json        TEXT             NOT NULL,  -- JSON-Array der Viertelstundensalden
    ae_preis_eur_kwh        DOUBLE PRECISION NOT NULL,
    netto_kosten_eur        DOUBLE PRECISION NOT NULL,
    status                  VARCHAR(20)      NOT NULL,
    erstellt_am             TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_abrechnungen_bilanzkreis ON abrechnungen (bilanzkreis_id);
CREATE INDEX idx_abrechnungen_periode     ON abrechnungen (bilanzkreis_id, periode_monat, periode_lauf);
