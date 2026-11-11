package fr.grandcinema.security;

import fr.grandcinema.domain.utilisateur.Utilisateur;
import fr.grandcinema.repository.utilisateur.UtilisateurRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Charge un utilisateur depuis la base pour Spring Security (l'e-mail sert d'identifiant). */
@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Utilisateur u = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
        return User.withUsername(u.getEmail())
                .password(u.getMotDePasse())
                .roles(u.getRole().name())          // devient l'autorité ROLE_CLIENT, ROLE_ADMIN...
                .disabled(!u.isActif())
                .build();
    }
}
