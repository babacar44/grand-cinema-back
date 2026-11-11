package fr.grandcinema.repository.catalogue;

import fr.grandcinema.domain.catalogue.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<fr.grandcinema.domain.catalogue.Place, Long> {

    List<Place> findBySalleIdOrderByRangeeAscNumeroAsc(Long salleId);
}
