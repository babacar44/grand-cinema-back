package fr.grandcinema.exception;

/** Renvoyée en HTTP 400 : la demande ne respecte pas une règle métier (trop de places, place inconnue...). */
public class RequeteInvalideException extends RuntimeException {

    public RequeteInvalideException(String message) {
        super(message);
    }
}
