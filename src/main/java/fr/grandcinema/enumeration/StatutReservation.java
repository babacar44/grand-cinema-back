package fr.grandcinema.enumeration;

public enum StatutReservation {
    /** Places bloquées 10 minutes, en attente de paiement (US 19). */
    EN_ATTENTE,
    CONFIRMEE,
    ANNULEE,
    EXPIREE,
    /** Billet scanné à l'entrée (US 32). */
    UTILISEE
}
