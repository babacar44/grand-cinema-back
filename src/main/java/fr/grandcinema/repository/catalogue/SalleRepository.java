package fr.grandcinema.repository.catalogue;

import fr.grandcinema.domain.catalogue.Salle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalleRepository extends JpaRepository<Salle, Long> {
}
