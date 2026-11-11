package fr.grandcinema.dto.reservation;

/** Une place du plan de salle et son état pour la séance (US 16). */
public record PlaceResponse(
        Long id,
        String rangee,
        Integer numero,
        boolean pmr,
        boolean libre
) {
}
