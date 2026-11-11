package fr.grandcinema.dto.reservation;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.grandcinema.domain.reservation.Reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Récapitulatif d'une réservation (US 20) et billet dématérialisé (US 30, US 31). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
        Long id,
        String statut,
        Long seanceId,
        String filmTitre,
        LocalDateTime debut,
        String salleNom,
        String typeSalle,
        List<String> places,
        BigDecimal montantTotal,
        LocalDateTime dateExpiration,
        UUID codeBillet
) {
    public static ReservationResponse de(Reservation r) {
        List<String> places = r.getPlaces().stream()
                .map(rp -> rp.getPlace())
                .sorted(Comparator.comparing((fr.grandcinema.domain.catalogue.Place p) -> p.getRangee())
                        .thenComparing(p -> p.getNumero()))
                .map(p -> p.getRangee() + p.getNumero())
                .toList();
        var seance = r.getSeance();
        return new ReservationResponse(
                r.getId(), r.getStatut().name(), seance.getId(), seance.getFilm().getTitre(),
                seance.getDebut(), seance.getSalle().getNom(), seance.getSalle().getTypeSalle().getLibelle(),
                places, r.getMontantTotal(), r.getDateExpiration(), r.getCodeBillet());
    }
}
