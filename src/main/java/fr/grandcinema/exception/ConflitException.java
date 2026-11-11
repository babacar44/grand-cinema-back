package fr.grandcinema.exception;

/** Renvoyée en HTTP 409 : la demande entre en conflit avec l'état actuel (séance qui chevauche, place déjà prise...). */
public class ConflitException extends RuntimeException {

    public ConflitException(String message) {
        super(message);
    }
}
