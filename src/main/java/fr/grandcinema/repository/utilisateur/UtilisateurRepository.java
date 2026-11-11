package fr.grandcinema.repository.utilisateur;

import fr.grandcinema.domain.utilisateur.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<fr.grandcinema.domain.utilisateur.Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
}
