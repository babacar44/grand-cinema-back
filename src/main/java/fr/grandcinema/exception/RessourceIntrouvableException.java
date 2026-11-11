package fr.grandcinema.exception;

/** Renvoyée en HTTP 404. */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
