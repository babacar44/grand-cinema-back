package fr.grandcinema.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** US 2 – Identifiants de connexion. */
public record ConnexionRequest(
        @NotBlank @Email String email,
        @NotBlank String motDePasse
) {
}
