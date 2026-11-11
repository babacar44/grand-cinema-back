package fr.grandcinema.domain.reservation;

import fr.grandcinema.domain.catalogue.Seance;
import fr.grandcinema.domain.utilisateur.Utilisateur;
import fr.grandcinema.enumeration.StatutReservation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    @Column(name = "montant_total", nullable = false, precision = 8, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    /** Contenu du QR code, généré à la confirmation (US 30). */
    @Column(name = "code_billet", unique = true)
    private UUID codeBillet;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    /** Fin du blocage des places (US 19). */
    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "date_utilisation")
    private LocalDateTime dateUtilisation;

    /** Verrouillage optimiste : empêche deux mises à jour concurrentes de la même réservation. */
    @Version
    @Column(nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationPlace> places = new ArrayList<>();

    public void ajouterPlace(ReservationPlace reservationPlace) {
        reservationPlace.setReservation(this);
        places.add(reservationPlace);
    }

    public boolean estExpiree(LocalDateTime maintenant) {
        return statut == StatutReservation.EN_ATTENTE && dateExpiration.isBefore(maintenant);
    }
}
