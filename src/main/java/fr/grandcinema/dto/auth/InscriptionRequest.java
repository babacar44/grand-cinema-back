package fr.grandcinema.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** US 1 – Données envoyées pour créer un compte. */
public record InscriptionRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial")
        String motDePasse
) {
}
