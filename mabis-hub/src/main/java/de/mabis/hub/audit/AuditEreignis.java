package de.mabis.hub.audit;

/**
 * Typisierte Geschäftsereignisse des MaBiS-Hubs.
 * Jedes Ereignis entspricht einer regulatorisch relevanten Operation.
 */
public enum AuditEreignis {

    // MSB-Operationen
    MESSWERT_EINGELIEFERT("MSB liefert Messwert ein"),
    MESSWERT_ABGELEHNT("Messwert abgelehnt – Validierungsfehler"),

    // VNB-Operationen
    STAMMDATEN_GEMELDET("VNB meldet Stammdaten"),
    STAMMDATEN_AKTUALISIERT("VNB aktualisiert Stammdaten"),

    // BKV-Operationen
    FAHRPLAN_EINGEREICHT("BKV reicht Tagesfahrplan ein"),
    FAHRPLAN_UEBERSCHRIEBEN("BKV überschreibt bestehenden Fahrplan"),
    ABRECHNUNG_ABGERUFEN("BKV ruft Abrechnung ab"),

    // ÜNB / Hub-intern
    ABRECHNUNGSLAUF_GESTARTET("ÜNB startet Abrechnungslauf"),
    ABRECHNUNGSLAUF_ABGESCHLOSSEN("Abrechnungslauf erfolgreich abgeschlossen"),
    ABRECHNUNGSLAUF_FEHLGESCHLAGEN("Abrechnungslauf fehlgeschlagen"),

    // Sicherheitsereignisse
    ZUGRIFF_VERWEIGERT("Zugriffsversuch ohne ausreichende Berechtigung"),
    UNBEKANNTE_MALO_ID("Messwert für unbekannte MaLo-ID eingeliefert");

    public final String beschreibung;

    AuditEreignis(String beschreibung) {
        this.beschreibung = beschreibung;
    }
}
