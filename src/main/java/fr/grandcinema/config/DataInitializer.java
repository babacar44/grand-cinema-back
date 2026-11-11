package fr.grandcinema.config;

import fr.grandcinema.domain.utilisateur.Utilisateur;
import fr.grandcinema.enumeration.Role;
import fr.grandcinema.repository.utilisateur.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Crée les comptes de démonstration au démarrage s'ils n'existent pas.
 * Les mots de passe viennent de variables d'environnement (valeurs de test par défaut).
 */
@Configuration
public class DataInitializer {

    @Bean
    ApplicationRunner creerComptesDemo(UtilisateurRepository repo, PasswordEncoder encoder,
                                       @Value("${app.demo.admin-password:Admin123!}") String adminPwd,
                                       @Value("${app.demo.client-password:Client123!}") String clientPwd,
                                       @Value("${app.demo.agent-password:Agent123!}") String agentPwd) {
        return args -> {
            creer(repo, encoder, "admin@grandcinema.fr", "Admin", "Cinéma", adminPwd, Role.ADMIN);
            creer(repo, encoder, "client@grandcinema.fr", "Dupont", "Marie", clientPwd, Role.CLIENT);
            creer(repo, encoder, "agent@grandcinema.fr", "Martin", "Paul", agentPwd, Role.AGENT);
        };
    }

    private void creer(UtilisateurRepository repo, PasswordEncoder encoder, String email,
                       String nom, String prenom, String motDePasse, Role role) {
        if (!repo.existsByEmail(email)) {
            Utilisateur u = new Utilisateur();
            u.setEmail(email);
            u.setNom(nom);
            u.setPrenom(prenom);
            u.setMotDePasse(encoder.encode(motDePasse));
            u.setRole(role);
            repo.save(u);
        }
    }
}
