package fr.grandcinema.repository.catalogue;

import fr.grandcinema.domain.catalogue.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FilmRepository extends JpaRepository<fr.grandcinema.domain.catalogue.Film, Long> {

    List<Film> findByActifTrueOrderByTitreAsc();

    @Query("""
           select f from Film f
           where f.actif = true
             and exists (select s from Seance s where s.film = f and s.debut > :maintenant)
           order by f.titre
           """)
    List<Film> findALaffiche(@Param("maintenant") LocalDateTime maintenant);

}
