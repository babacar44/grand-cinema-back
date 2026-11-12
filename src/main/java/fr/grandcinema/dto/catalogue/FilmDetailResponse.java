package fr.grandcinema.dto.catalogue;

import java.util.List;

/** Fiche détaillée d'un film avec ses prochaines séances (US 10). */
public record FilmDetailResponse(
        FilmResponse film,
        List<SeanceResponse> seances
) {
}
