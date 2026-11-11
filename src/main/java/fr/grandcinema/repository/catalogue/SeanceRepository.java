package fr.grandcinema.repository.catalogue;

import fr.grandcinema.domain.catalogue.Seance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    /** Séances à venir d'un film (US 11), avec la salle et son type chargés en une requête. */
    @EntityGraph(attributePaths = {"salle", "salle.typeSalle"})
    List<Seance> findByFilmIdAndDebutAfterOrderByDebutAsc(Long filmId, LocalDateTime apres);
}
