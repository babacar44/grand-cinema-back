package fr.grandcinema.dto.catalogue;

import jakarta.validation.constraints.*;

/** Création / modification d'un film par l'administrateur (US 36). */
public record FilmRequest(
        @NotBlank @Size(max = 200) String titre,
        String synopsis,
        @NotNull @Min(1) @Max(600) Integer dureeMinutes,
        @Size(max = 50) String genre,
        @Size(max = 150) String realisateur,
        @NotNull @Min(0) @Max(18) Integer ageMinimum,
        @Size(max = 500) String afficheUrl
) {
}
