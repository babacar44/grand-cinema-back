package fr.grandcinema.dto.catalogue;

import fr.grandcinema.domain.catalogue.Film;

/** Film affiché dans la liste et la fiche (US 9, US 10). */
public record FilmResponse(
        Long id,
        String titre,
        String synopsis,
        Integer dureeMinutes,
        String genre,
        String realisateur,
        Integer ageMinimum,
        String afficheUrl
) {
    public static FilmResponse de(Film f) {
        return new FilmResponse(f.getId(), f.getTitre(), f.getSynopsis(), f.getDureeMinutes(),
                f.getGenre(), f.getRealisateur(), f.getAgeMinimum(), f.getAfficheUrl());
    }
}
