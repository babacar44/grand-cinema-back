package fr.grandcinema.dto.catalogue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Séance avec ce dont le client a besoin pour choisir (US 11) : horaire, salle, type, prix, places restantes. */
public record SeanceResponse(
        Long id,
        Long filmId,
        String filmTitre,
        LocalDateTime debut,
        LocalDateTime fin,
        String version,
        Long salleId,
        String salleNom,
        String typeSalle,
        BigDecimal prix,
        long placesDisponibles,
        boolean complet
) {
}
