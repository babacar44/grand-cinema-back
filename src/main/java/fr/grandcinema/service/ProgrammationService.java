package fr.grandcinema.service;

import fr.grandcinema.domain.catalogue.Film;
import fr.grandcinema.domain.catalogue.Salle;
import fr.grandcinema.domain.catalogue.Seance;
import fr.grandcinema.dto.catalogue.FilmRequest;
import fr.grandcinema.dto.catalogue.FilmResponse;
import fr.grandcinema.dto.catalogue.SeanceRequest;
import fr.grandcinema.dto.catalogue.SeanceResponse;
import fr.grandcinema.enumeration.VersionFilm;
import fr.grandcinema.exception.ConflitException;
import fr.grandcinema.exception.RessourceIntrouvableException;
import fr.grandcinema.repository.catalogue.FilmRepository;
import fr.grandcinema.repository.catalogue.SalleRepository;
import fr.grandcinema.repository.catalogue.SeanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Back-office de programmation : US 36 (films) et US 38 (séances). */
@Service
@Transactional
public class ProgrammationService {

    /** Battement entre deux séances pour le nettoyage de la salle (critère d'acceptation US 38). */
    static final int BATTEMENT_MINUTES = 20;

    private final FilmRepository filmRepository;
    private final SalleRepository salleRepository;
    private final SeanceRepository seanceRepository;
    private final CatalogueService catalogueService;

    public ProgrammationService(FilmRepository filmRepository, SalleRepository salleRepository,
                                SeanceRepository seanceRepository, CatalogueService catalogueService) {
        this.filmRepository = filmRepository;
        this.salleRepository = salleRepository;
        this.seanceRepository = seanceRepository;
        this.catalogueService = catalogueService;
    }

    // ---------- US 36 : films ----------

    @Transactional(readOnly = true)
    public List<FilmResponse> tousLesFilms() {
        return filmRepository.findAll().stream().map(FilmResponse::de).toList();
    }

    public FilmResponse creerFilm(FilmRequest req) {
        Film film = new Film();
        appliquer(film, req);
        return FilmResponse.de(filmRepository.save(film));
    }

    public FilmResponse modifierFilm(Long id, FilmRequest req) {
        Film film = film(id);
        appliquer(film, req);
        return FilmResponse.de(film);
    }

    /** Archivage plutôt que suppression : les réservations passées restent cohérentes. */
    public void archiverFilm(Long id) {
        film(id).setActif(false);
    }

    // ---------- US 38 : séances ----------

    public SeanceResponse creerSeance(SeanceRequest req) {
        Film film = film(req.filmId());
        if (!film.isActif()) {
            throw new ConflitException("Impossible de programmer un film archivé");
        }
        Salle salle = salleRepository.findById(req.salleId())
                .filter(Salle::isActive)
                .orElseThrow(() -> new RessourceIntrouvableException("Salle introuvable ou indisponible"));

        LocalDateTime fin = calculerFin(req.debut(), film.getDureeMinutes());
        if (seanceRepository.existeChevauchement(salle.getId(), req.debut(), fin)) {
            throw new ConflitException("Une séance est déjà programmée dans cette salle sur ce créneau");
        }

        Seance seance = new Seance();
        seance.setFilm(film);
        seance.setSalle(salle);
        seance.setDebut(req.debut());
        seance.setFin(fin);
        seance.setVersion(req.version() != null ? req.version() : VersionFilm.VF);
        // la contrainte EXCLUDE en base reste le filet de sécurité si deux admins programment en même temps
        return catalogueService.versReponse(seanceRepository.save(seance));
    }

    static LocalDateTime calculerFin(LocalDateTime debut, int dureeMinutes) {
        return debut.plusMinutes(dureeMinutes + BATTEMENT_MINUTES);
    }

    private Film film(Long id) {
        return filmRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Film introuvable"));
    }

    private static void appliquer(Film film, FilmRequest req) {
        film.setTitre(req.titre().trim());
        film.setSynopsis(req.synopsis());
        film.setDureeMinutes(req.dureeMinutes());
        film.setGenre(req.genre());
        film.setRealisateur(req.realisateur());
        film.setAgeMinimum(req.ageMinimum());
        film.setAfficheUrl(req.afficheUrl());
    }
}
