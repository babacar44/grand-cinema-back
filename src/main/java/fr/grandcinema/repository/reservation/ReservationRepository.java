package fr.grandcinema.repository.reservation;

import fr.grandcinema.domain.reservation.Reservation;
import fr.grandcinema.enumeration.StatutReservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** « Mes réservations » (US 31). */
    List<Reservation> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);

    /** Réservations dont le blocage de 10 minutes est dépassé (libération des places). */
    List<Reservation> findByStatutAndDateExpirationBefore(StatutReservation statut, LocalDateTime date);

    long countBySeanceId(Long seanceId);

    /** US 31 : réservations d'un client, avec tout ce qu'il faut pour l'affichage en une requête. */
    @EntityGraph(attributePaths = {"seance", "seance.film", "seance.salle", "seance.salle.typeSalle", "places", "places.place"})
    List<Reservation> findByUtilisateurEmailOrderByDateCreationDesc(String email);

    /** Réservations expirées d'une séance (libérées avant d'afficher le plan ou de réserver). */
    List<Reservation> findBySeanceIdAndStatutAndDateExpirationBefore(Long seanceId, StatutReservation statut, LocalDateTime date);

}
