package fr.grandcinema.service;

import fr.grandcinema.domain.catalogue.Film;
import fr.grandcinema.domain.catalogue.Seance;
import fr.grandcinema.dto.catalogue.FilmDetailResponse;
import fr.grandcinema.dto.catalogue.FilmResponse;
import fr.grandcinema.dto.catalogue.SeanceResponse;
import fr.grandcinema.exception.RessourceIntrouvableException;
import fr.grandcinema.repository.catalogue.FilmRepository;
import fr.grandcinema.repository.catalogue.SeanceRepository;
import fr.grandcinema.repository.reservation.ReservationPlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Consultation publique du catalogue : US 9, 10 et 11. */
@Service
@Transactional(readOnly = true)
public class CatalogueService {

    /** Les séances sont proposées sur les 7 prochains jours (critère d'acceptation US 11). */
    static final int JOURS_AFFICHES = 7;

    private final FilmRepository filmRepository;
    private final SeanceRepository seanceRepository;
    private final ReservationPlaceRepository reservationPlaceRepository;
    private final Clock clock;

    public CatalogueService(FilmRepository filmRepository, SeanceRepository seanceRepository,
                            ReservationPlaceRepository reservationPlaceRepository, Clock clock) {
        this.filmRepository = filmRepository;
        this.seanceRepository = seanceRepository;
        this.reservationPlaceRepository = reservationPlaceRepository;
        this.clock = clock;
    }

    /** US 9 – Films à l'affiche : actifs et avec au moins une séance à venir. */
    public List<FilmResponse> filmsALaffiche() {
        return filmRepository.findALaffiche(LocalDateTime.now(clock)).stream()
                .map(FilmResponse::de)
                .toList();
    }

    /** US 10 – Fiche d'un film avec ses prochaines séances. */
    public FilmDetailResponse ficheFilm(Long filmId) {
        Film film = filmRepository.findById(filmId)
                .filter(Film::isActif)
                .orElseThrow(() -> new RessourceIntrouvableException("Film introuvable"));
        return new FilmDetailResponse(FilmResponse.de(film), seancesDuFilm(filmId));
    }

    /** US 11 – Séances à venir d'un film sur les 7 prochains jours (les séances passées ne sont pas affichées). */
    public List<SeanceResponse> seancesDuFilm(Long filmId) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return seanceRepository
                .findByFilmIdAndDebutBetweenOrderByDebutAsc(filmId, maintenant, maintenant.plusDays(JOURS_AFFICHES))
                .stream()
                .map(this::versReponse)
                .toList();
    }

    public SeanceResponse seance(Long seanceId) {
        return seanceRepository.findById(seanceId)
                .map(this::versReponse)
                .orElseThrow(() -> new RessourceIntrouvableException("Séance introuvable"));
    }

    SeanceResponse versReponse(Seance s) {
        long occupees = reservationPlaceRepository.countBySeanceId(s.getId());
        long disponibles = Math.max(0, s.getSalle().getCapacite() - occupees);
        return new SeanceResponse(
                s.getId(), s.getFilm().getId(), s.getFilm().getTitre(),
                s.getDebut(), s.getFin(), s.getVersion().name(),
                s.getSalle().getId(), s.getSalle().getNom(),
                s.getSalle().getTypeSalle().getLibelle(), s.getSalle().getTypeSalle().getPrix(),
                disponibles, disponibles == 0);
    }
}
