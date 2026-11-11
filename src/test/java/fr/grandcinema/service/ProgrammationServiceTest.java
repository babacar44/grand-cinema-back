package fr.grandcinema.service;

import fr.grandcinema.domain.catalogue.Film;
import fr.grandcinema.domain.catalogue.Salle;
import fr.grandcinema.dto.catalogue.SeanceRequest;
import fr.grandcinema.exception.ConflitException;
import fr.grandcinema.repository.catalogue.FilmRepository;
import fr.grandcinema.repository.catalogue.SalleRepository;
import fr.grandcinema.repository.catalogue.SeanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Tests de l'US 38 : calcul de la fin de séance et refus des chevauchements. */
@ExtendWith(MockitoExtension.class)
class ProgrammationServiceTest {

    @Mock FilmRepository filmRepository;
    @Mock SalleRepository salleRepository;
    @Mock SeanceRepository seanceRepository;
    @Mock CatalogueService catalogueService;
    @InjectMocks ProgrammationService service;

    private final LocalDateTime debut = LocalDateTime.of(2030, 7, 14, 20, 30);

    @Test
    void laFinDeSeance_ajouteLaDureeDuFilmEtLeBattement() {
        // film de 112 min + 20 min de battement = fin à 22h42
        assertThat(ProgrammationService.calculerFin(debut, 112))
                .isEqualTo(LocalDateTime.of(2030, 7, 14, 22, 42));
    }

    @Test
    void uneSeanceQuiChevaucheEstRefusee() {
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film(112)));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle()));
        when(seanceRepository.existeChevauchement(eq(1L), eq(debut), any())).thenReturn(true);

        assertThatThrownBy(() -> service.creerSeance(new SeanceRequest(1L, 1L, debut, null)))
                .isInstanceOf(ConflitException.class);
        verify(seanceRepository, never()).save(any());
    }

    @Test
    void unFilmArchiveNePeutPasEtreProgramme() {
        Film archive = film(112);
        archive.setActif(false);
        when(filmRepository.findById(1L)).thenReturn(Optional.of(archive));

        assertThatThrownBy(() -> service.creerSeance(new SeanceRequest(1L, 1L, debut, null)))
                .isInstanceOf(ConflitException.class);
    }

    private static Film film(int duree) {
        Film f = new Film();
        f.setId(1L);
        f.setTitre("Les Notes du Fleuve");
        f.setDureeMinutes(duree);
        return f;
    }

    private static Salle salle() {
        Salle s = new Salle();
        s.setId(1L);
        s.setNom("Salle 1");
        s.setCapacite(300);
        return s;
    }
}
