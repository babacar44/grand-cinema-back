package fr.grandcinema.dto.auth;

/** Réponse après inscription ou connexion : le jeton JWT et les infos utiles au front. */
public record AuthResponse(
        String token,
        Long id,
        String email,
        String nom,
        String prenom,
        String role
) {
}
