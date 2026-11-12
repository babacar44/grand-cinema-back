package fr.grandcinema.repository.catalogue;

import fr.grandcinema.domain.catalogue.Seance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    /** Séances à venir d'un film (US 11), avec la salle et son type chargés en une requête. */
    @EntityGraph(attributePaths = {"salle", "salle.typeSalle"})
    List<Seance> findByFilmIdAndDebutAfterOrderByDebutAsc(Long filmId, LocalDateTime apres);

    @EntityGraph(attributePaths = {"film", "salle", "salle.typeSalle"})
    List<Seance> findByFilmIdAndDebutBetweenOrderByDebutAsc(Long filmId, LocalDateTime debut, LocalDateTime fin);

    /** Deux créneaux se chevauchent si l'un commence avant la fin de l'autre et inversement. */
    @Query("""
           select count(s) > 0 from Seance s
           where s.salle.id = :salleId and s.debut < :fin and s.fin > :debut
           """)
    boolean existeChevauchement(@Param("salleId") Long salleId,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin);


}
