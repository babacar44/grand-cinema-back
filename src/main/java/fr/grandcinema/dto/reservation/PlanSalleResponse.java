package fr.grandcinema.dto.reservation;

import java.math.BigDecimal;
import java.util.List;

/** Plan de salle d'une séance avec le prix d'une place (US 16, US 18). */
public record PlanSalleResponse(
        Long seanceId,
        String salleNom,
        String typeSalle,
        BigDecimal prixPlace,
        long placesLibres,
        List<PlaceResponse> places
) {
}
