package fr.grandcinema.dto.catalogue;

import fr.grandcinema.enumeration.VersionFilm;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Programmation d'une séance par l'administrateur (US 38). La fin est calculée automatiquement. */
public record SeanceRequest(
        @NotNull Long filmId,
        @NotNull Long salleId,
        @NotNull @Future LocalDateTime debut,
        VersionFilm version
) {
}
