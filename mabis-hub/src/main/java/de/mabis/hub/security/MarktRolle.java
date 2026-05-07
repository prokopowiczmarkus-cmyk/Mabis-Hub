package de.mabis.hub.security;

/**
 * Marktrollen gemäß MaBiS/BDEW-Rollenmodell.
 * Jede Rolle entspricht genau einer gesetzlich definierten Marktfunktion.
 *
 * Vergabe erfolgt durch den Identity Provider (Keycloak) im JWT-Claim "roles".
 */
public final class MarktRolle {

    private MarktRolle() {}

    /** Messstellenbetreiber – darf Messwerte einliefern. */
    public static final String MSB  = "ROLE_MSB";

    /** Verteilnetzbetreiber – darf Stammdaten melden. */
    public static final String VNB  = "ROLE_VNB";

    /** Bilanzkreisverantwortlicher – darf Fahrpläne einreichen und Abrechnungen abrufen. */
    public static final String BKV  = "ROLE_BKV";

    /** Übertragungsnetzbetreiber – darf Abrechnungsläufe anstoßen (Hub-Betreiber). */
    public static final String UNB  = "ROLE_UNB";

    /** Regulierungsbehörde / Admin – Lesezugriff auf alle Endpunkte. */
    public static final String ADMIN = "ROLE_ADMIN";
}
