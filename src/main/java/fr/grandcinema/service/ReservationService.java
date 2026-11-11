package fr.grandcinema.service;

import fr.grandcinema.domain.catalogue.Place;
import fr.grandcinema.domain.catalogue.Seance;
import fr.grandcinema.domain.reservation.Reservation;
import fr.grandcinema.domain.reservation.ReservationPlace;
import fr.grandcinema.domain.utilisateur.Utilisateur;
import fr.grandcinema.dto.reservation.PlaceResponse;
import fr.grandcinema.dto.reservation.PlanSalleResponse;
import fr.grandcinema.dto.reservation.ReservationRequest;
import fr.grandcinema.dto.reservation.ReservationResponse;
import fr.grandcinema.enumeration.StatutReservation;
import fr.grandcinema.exception.ConflitException;
import fr.grandcinema.exception.RequeteInvalideException;
import fr.grandcinema.exception.RessourceIntrouvableException;
import fr.grandcinema.repository.catalogue.PlaceRepository;
import fr.grandcinema.repository.catalogue.SeanceRepository;
import fr.grandcinema.repository.reservation.ReservationPlaceRepository;
import fr.grandcinema.repository.reservation.ReservationRepository;
import fr.grandcinema.repository.utilisateur.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/** Réservation de places : US 16 à 20 (réserver), US 30 et 31 (billet, mes réservations). */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    /** US 17 : 10 places maximum par réservation. */
    static final int MAX_PLACES = 10;

    private final ReservationRepository reservationRepository;
    private final ReservationPlaceRepository reservationPlaceRepository;
    private final SeanceRepository seanceRepository;
    private final PlaceRepository placeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final Clock clock;
    private final int blocageMinutes;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationPlaceRepository reservationPlaceRepository,
                              SeanceRepository seanceRepository, PlaceRepository placeRepository,
                              UtilisateurRepository utilisateurRepository, Clock clock,
                              @Value("${app.reservation.blocage-minutes}") int blocageMinutes) {
        this.reservationRepository = reservationRepository;
        this.reservationPlaceRepository = reservationPlaceRepository;
        this.seanceRepository = seanceRepository;
        this.placeRepository = placeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.clock = clock;
        this.blocageMinutes = blocageMinutes;
    }

    /** US 16 – Plan de salle : chaque place avec son état (libre ou occupée) pour la séance. */
    @Transactional
    public PlanSalleResponse planDeSalle(Long seanceId) {
        Seance seance = seance(seanceId);
        libererExpirees(seanceId);
        Set<Long> occupees = new HashSet<>(reservationPlaceRepository.findPlaceIdsOccupees(seanceId));
        List<PlaceResponse> places = placeRepository.findBySalleIdOrderByRangeeAscNumeroAsc(seance.getSalle().getId())
                .stream()
                .map(p -> new PlaceResponse(p.getId(), p.getRangee(), p.getNumero(), p.isPmr(), !occupees.contains(p.getId())))
                .toList();
        long libres = places.stream().filter(PlaceResponse::libre).count();
        return new PlanSalleResponse(seanceId, seance.getSalle().getNom(),
                seance.getSalle().getTypeSalle().getLibelle(), seance.getSalle().getTypeSalle().getPrix(),
                libres, places);
    }

    /**
     * US 17, 18, 19, 20 – Réserver des places : elles sont bloquées pendant {@code blocageMinutes}
     * en attendant le paiement. Deux protections contre la double réservation :
     * une vérification applicative, puis la contrainte d'unicité en base (seance_id, place_id).
     */
    @Transactional
    public ReservationResponse reserver(String email, ReservationRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        Seance seance = seance(request.seanceId());
        LocalDateTime maintenant = LocalDateTime.now(clock);
        if (!seance.getDebut().isAfter(maintenant)) {
            throw new ConflitException("Cette séance a déjà commencé");
        }

        Set<Long> placeIds = new LinkedHashSet<>(request.placeIds());
        if (placeIds.isEmpty() || placeIds.size() > MAX_PLACES) {
            throw new RequeteInvalideException("Vous pouvez réserver entre 1 et " + MAX_PLACES + " places");
        }
        List<Place> places = placeRepository.findAllById(placeIds);
        boolean placesValides = places.size() == placeIds.size()
                && places.stream().allMatch(p -> p.getSalle().getId().equals(seance.getSalle().getId()));
        if (!placesValides) {
            throw new RequeteInvalideException("Une ou plusieurs places n'existent pas dans cette salle");
        }

        libererExpirees(seance.getId());
        if (reservationPlaceRepository.existsBySeanceIdAndPlaceIdIn(seance.getId(), placeIds)) {
            throw new ConflitException("Une ou plusieurs places viennent d'être réservées par un autre client");
        }

        // US 18 : prix selon le type de salle
        BigDecimal prix = seance.getSalle().getTypeSalle().getPrix();
        Reservation reservation = new Reservation();
        reservation.setUtilisateur(utilisateur);
        reservation.setSeance(seance);
        reservation.setDateCreation(maintenant);
        reservation.setDateExpiration(maintenant.plusMinutes(blocageMinutes));   // US 19
        for (Place place : places) {
            ReservationPlace rp = new ReservationPlace();
            rp.setSeanceId(seance.getId());
            rp.setPlace(place);
            rp.setPrix(prix);
            reservation.ajouterPlace(rp);
        }
        reservation.setMontantTotal(prix.multiply(BigDecimal.valueOf(places.size())));

        try {
            reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            // Deux clients ont validé la même place au même instant : la base refuse le second
            throw new ConflitException("Une ou plusieurs places viennent d'être réservées par un autre client");
        }
        return ReservationResponse.de(reservation);
    }

    /** Paiement simulé (le paiement réel Stripe est prévu en V2) : confirme la réservation et génère le billet (US 30). */
    @Transactional
    public ReservationResponse confirmer(String email, Long reservationId) {
        Reservation r = reservationDe(email, reservationId);
        if (r.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new ConflitException("Cette réservation ne peut plus être confirmée");
        }
        if (r.estExpiree(LocalDateTime.now(clock))) {
            throw new ConflitException("Le délai de " + blocageMinutes + " minutes est dépassé, les places ont été libérées");
        }
        r.setStatut(StatutReservation.CONFIRMEE);
        r.setCodeBillet(UUID.randomUUID());   // contenu du QR code
        return ReservationResponse.de(r);
    }

    /** Abandon d'une réservation en attente : les places sont libérées immédiatement. */
    @Transactional
    public void annuler(String email, Long reservationId) {
        Reservation r = reservationDe(email, reservationId);
        if (r.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new ConflitException("Seule une réservation en attente de paiement peut être abandonnée");
        }
        liberer(r, StatutReservation.ANNULEE);
    }

    /** US 31 – Mes réservations (la plus récente en premier). */
    @Transactional(readOnly = true)
    public List<ReservationResponse> mesReservations(String email) {
        return reservationRepository.findByUtilisateurEmailOrderByDateCreationDesc(email).stream()
                .map(ReservationResponse::de)
                .toList();
    }

    /** US 30 – Détail d'une réservation avec son code de billet. */
    @Transactional(readOnly = true)
    public ReservationResponse detail(String email, Long reservationId) {
        return ReservationResponse.de(reservationDe(email, reservationId));
    }

    /** US 19 – Toutes les minutes, les places dont le blocage a expiré sont libérées. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void libererToutesLesExpirees() {
        List<Reservation> expirees = reservationRepository
                .findByStatutAndDateExpirationBefore(StatutReservation.EN_ATTENTE, LocalDateTime.now(clock));
        expirees.forEach(r -> liberer(r, StatutReservation.EXPIREE));
        if (!expirees.isEmpty()) {
            log.info("{} réservation(s) expirée(s) libérée(s)", expirees.size());
        }
    }

    private void libererExpirees(Long seanceId) {
        reservationRepository.findBySeanceIdAndStatutAndDateExpirationBefore(
                        seanceId, StatutReservation.EN_ATTENTE, LocalDateTime.now(clock))
                .forEach(r -> liberer(r, StatutReservation.EXPIREE));
        // Hibernate exécute les INSERT avant les DELETE lors du flush : on force la suppression
        // des places libérées maintenant, avant d'en réserver de nouvelles.
        reservationRepository.flush();
    }

    private void liberer(Reservation r, StatutReservation nouveauStatut) {
        r.setStatut(nouveauStatut);
        r.getPlaces().clear();   // orphanRemoval : les lignes reservation_place sont supprimées
    }

    /** Un client ne voit que ses propres réservations (sinon 404, pour ne rien révéler). */
    private Reservation reservationDe(String email, Long reservationId) {
        return reservationRepository.findById(reservationId)
                .filter(r -> r.getUtilisateur().getEmail().equals(email))
                .orElseThrow(() -> new RessourceIntrouvableException("Réservation introuvable"));
    }

    private Seance seance(Long seanceId) {
        return seanceRepository.findById(seanceId)
                .orElseThrow(() -> new RessourceIntrouvableException("Séance introuvable"));
    }
}
