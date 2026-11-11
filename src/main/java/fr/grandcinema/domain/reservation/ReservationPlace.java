package fr.grandcinema.domain.reservation;

import fr.grandcinema.domain.catalogue.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Place réservée pour une séance.
 * La contrainte en base uk_place_par_seance (seance_id, place_id) interdit la double réservation.
 */
@Entity
@Table(name = "reservation_place")
@Getter
@Setter
@NoArgsConstructor
public class ReservationPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    /** Copie de l'identifiant de la séance : nécessaire à la contrainte anti double réservation. */
    @Column(name = "seance_id", nullable = false)
    private Long seanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal prix;
}
