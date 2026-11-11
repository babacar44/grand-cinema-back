package fr.grandcinema.repository.catalogue;

import fr.grandcinema.domain.catalogue.Film;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FilmRepository extends JpaRepository<fr.grandcinema.domain.catalogue.Film, Long> {

    List<Film> findByActifTrueOrderByTitreAsc();
}
