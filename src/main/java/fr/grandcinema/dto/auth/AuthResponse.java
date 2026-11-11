package fr.grandcinema.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Réponse après inscription ou connexion : le jeton JWT et les infos utiles au front. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String token,
        Long id,
        String email,
        String nom,
        String prenom,
        String role
) {
}
