package fr.grandcinema.repository.reservation;

import fr.grandcinema.domain.reservation.Reservation;
import fr.grandcinema.enumeration.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** « Mes réservations » (US 31). */
    List<Reservation> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);

    /** Réservations dont le blocage de 10 minutes est dépassé (libération des places). */
    List<Reservation> findByStatutAndDateExpirationBefore(StatutReservation statut, LocalDateTime date);

    long countBySeanceId(Long seanceId);
}
