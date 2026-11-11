package fr.grandcinema.service;

import fr.grandcinema.domain.catalogue.*;
import fr.grandcinema.domain.reservation.Reservation;
import fr.grandcinema.domain.utilisateur.Utilisateur;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/** Tests unitaires de la réservation (US 17 à 20, 30) : les règles métier, sans base de données. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceTest {

    /** Horloge figée : le 14/07/2030 à 18h00 (heure de Paris). */
    private static final Clock HORLOGE =
            Clock.fixed(Instant.parse("2030-07-14T16:00:00Z"), ZoneId.of("Europe/Paris"));
    private static final LocalDateTime MAINTENANT = LocalDateTime.now(HORLOGE);

    @Mock ReservationRepository reservationRepository;
    @Mock ReservationPlaceRepository reservationPlaceRepository;
    @Mock SeanceRepository seanceRepository;
    @Mock PlaceRepository placeRepository;
    @Mock UtilisateurRepository utilisateurRepository;

    ReservationService service;
    Utilisateur client;
    Salle salleVip;
    Seance seance;

    @BeforeEach
    void preparer() {
        service = new ReservationService(reservationRepository, reservationPlaceRepository, seanceRepository,
                placeRepository, utilisateurRepository, HORLOGE, 10);

        client = new Utilisateur();
        client.setId(1L);
        client.setEmail("client@grandcinema.fr");

        TypeSalle vip = new TypeSalle();
        vip.setCode("VIP");
        vip.setLibelle("VIP");
        vip.setPrix(new BigDecimal("18.00"));
        salleVip = new Salle();
        salleVip.setId(7L);
        salleVip.setNom("Salle 7");
        salleVip.setTypeSalle(vip);

        Film film = new Film();
        film.setId(1L);
        film.setTitre("Les Notes du Fleuve");
        seance = new Seance();
        seance.setId(100L);
        seance.setFilm(film);
        seance.setSalle(salleVip);
        seance.setDebut(MAINTENANT.plusHours(2));

        when(utilisateurRepository.findByEmail("client@grandcinema.fr")).thenReturn(Optional.of(client));
        when(seanceRepository.findById(100L)).thenReturn(Optional.of(seance));
        when(reservationRepository.findBySeanceIdAndStatutAndDateExpirationBefore(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void reserver_calculeLeMontantSelonLeTypeDeSalle_etBloqueLesPlacesDixMinutes() {
        when(placeRepository.findAllById(any())).thenReturn(List.of(place(1L, salleVip), place(2L, salleVip)));
        when(reservationPlaceRepository.existsBySeanceIdAndPlaceIdIn(eq(100L), anyCollection())).thenReturn(false);

        ReservationResponse r = service.reserver("client@grandcinema.fr", new ReservationRequest(100L, List.of(1L, 2L)));

        assertThat(r.montantTotal()).isEqualByComparingTo("36.00");           // 2 places VIP x 18 €
        assertThat(r.statut()).isEqualTo("EN_ATTENTE");
        assertThat(r.dateExpiration()).isEqualTo(MAINTENANT.plusMinutes(10));  // US 19
        assertThat(r.places()).containsExactly("A1", "A2");
        verify(reservationRepository).saveAndFlush(any(Reservation.class));
    }

    @Test
    void reserver_refuseUnePlaceDejaReservee() {
        when(placeRepository.findAllById(any())).thenReturn(List.of(place(1L, salleVip)));
        when(reservationPlaceRepository.existsBySeanceIdAndPlaceIdIn(eq(100L), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.reserver("client@grandcinema.fr", new ReservationRequest(100L, List.of(1L))))
                .isInstanceOf(ConflitException.class);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void reserver_transformeLaViolationDeContrainteEnConflit_casDeDeuxClientsSimultanes() {
        when(placeRepository.findAllById(any())).thenReturn(List.of(place(1L, salleVip)));
        when(reservationPlaceRepository.existsBySeanceIdAndPlaceIdIn(eq(100L), anyCollection())).thenReturn(false);
        when(reservationRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_place_par_seance"));

        assertThatThrownBy(() -> service.reserver("client@grandcinema.fr", new ReservationRequest(100L, List.of(1L))))
                .isInstanceOf(ConflitException.class);
    }

    @Test
    void reserver_refusePlusDeDixPlaces() {
        List<Long> onzePlaces = LongStream.rangeClosed(1, 11).boxed().toList();

        assertThatThrownBy(() -> service.reserver("client@grandcinema.fr", new ReservationRequest(100L, onzePlaces)))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void reserver_refuseUnePlaceDUneAutreSalle() {
        Salle autreSalle = new Salle();
        autreSalle.setId(1L);
        when(placeRepository.findAllById(any())).thenReturn(List.of(place(999L, autreSalle)));

        assertThatThrownBy(() -> service.reserver("client@grandcinema.fr", new ReservationRequest(100L, List.of(999L))))
                .isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void reserver_refuseUneSeanceDejaCommencee() {
        seance.setDebut(MAINTENANT.minusMinutes(5));

        assertThatThrownBy(() -> service.reserver("client@grandcinema.fr", new ReservationRequest(100L, List.of(1L))))
                .isInstanceOf(ConflitException.class);
    }

    @Test
    void confirmer_genereUnCodeDeBillet() {
        Reservation r = reservationEnAttente(MAINTENANT.plusMinutes(5));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(r));

        ReservationResponse reponse = service.confirmer("client@grandcinema.fr", 50L);

        assertThat(reponse.statut()).isEqualTo("CONFIRMEE");
        assertThat(reponse.codeBillet()).isNotNull();
    }

    @Test
    void confirmer_refuseApresLeDelaiDeDixMinutes() {
        Reservation r = reservationEnAttente(MAINTENANT.minusMinutes(1));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.confirmer("client@grandcinema.fr", 50L))
                .isInstanceOf(ConflitException.class);
    }

    @Test
    void unClientNePeutPasConfirmerLaReservationDUnAutre() {
        Reservation r = reservationEnAttente(MAINTENANT.plusMinutes(5));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.confirmer("pirate@exemple.fr", 50L))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void lesReservationsExpireesSontLibereesAutomatiquement() {
        Reservation expiree = reservationEnAttente(MAINTENANT.minusMinutes(1));
        when(reservationRepository.findByStatutAndDateExpirationBefore(StatutReservation.EN_ATTENTE, MAINTENANT))
                .thenReturn(List.of(expiree));

        service.libererToutesLesExpirees();

        assertThat(expiree.getStatut()).isEqualTo(StatutReservation.EXPIREE);
        assertThat(expiree.getPlaces()).isEmpty();
    }

    private Reservation reservationEnAttente(LocalDateTime expiration) {
        Reservation r = new Reservation();
        r.setId(50L);
        r.setUtilisateur(client);
        r.setSeance(seance);
        r.setDateExpiration(expiration);
        return r;
    }

    private static Place place(Long id, Salle salle) {
        Place p = new Place();
        p.setId(id);
        p.setSalle(salle);
        p.setRangee("A");
        p.setNumero(id.intValue());
        return p;
    }
}
