package fr.grandcinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestion centralisée des erreurs : chaque erreur est renvoyée au format standard
 * « Problem Details » (RFC 9457), sans jamais exposer de détail technique au client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erreurs.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Données invalides");
        pd.setProperty("erreurs", erreurs);
        return pd;
    }

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ProblemDetail handleEmailDejaUtilise(EmailDejaUtiliseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** US 2 : message générique, on ne dit pas si c'est l'e-mail ou le mot de passe qui est faux. */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ProblemDetail handleIdentifiants() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Identifiants incorrects");
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail handleIntrouvable(RessourceIntrouvableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflitException.class)
    public ProblemDetail handleConflit(ConflitException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

}
