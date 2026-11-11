package fr.grandcinema.exception;

public class EmailDejaUtiliseException extends RuntimeException {

    public EmailDejaUtiliseException(String email) {
        super("Un compte existe déjà avec l'adresse " + email);
    }
}
