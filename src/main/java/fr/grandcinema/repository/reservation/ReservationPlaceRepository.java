package fr.grandcinema.repository.reservation;

import fr.grandcinema.domain.reservation.ReservationPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ReservationPlaceRepository extends JpaRepository<ReservationPlace, Long> {

    /** Identifiants des places déjà prises pour une séance (plan de salle, US 16). */
    @Query("select rp.place.id from ReservationPlace rp where rp.seanceId = :seanceId")
    List<Long> findPlaceIdsOccupees(Long seanceId);

    boolean existsBySeanceIdAndPlaceIdIn(Long seanceId, Collection<Long> placeIds);

    long countBySeanceId(Long seanceId);
}
