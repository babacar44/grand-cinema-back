package fr.grandcinema.service;

import fr.grandcinema.domain.utilisateur.Utilisateur;
import fr.grandcinema.dto.auth.AuthResponse;
import fr.grandcinema.dto.auth.ConnexionRequest;
import fr.grandcinema.dto.auth.InscriptionRequest;
import fr.grandcinema.enumeration.Role;
import fr.grandcinema.exception.EmailDejaUtiliseException;
import fr.grandcinema.repository.utilisateur.UtilisateurRepository;
import fr.grandcinema.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** US 1 – Inscription : e-mail unique, mot de passe chiffré, rôle CLIENT par défaut (US 4). */
    @Transactional
    public AuthResponse inscrire(InscriptionRequest request) {
        String email = request.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmail(email)) {
            throw new EmailDejaUtiliseException(email);
        }
        Utilisateur u = new Utilisateur();
        u.setNom(request.nom().trim());
        u.setPrenom(request.prenom().trim());
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode(request.motDePasse()));
        u.setRole(Role.CLIENT);
        return reponse(utilisateurRepository.save(u));
    }

    /** US 2 – Connexion : vérification par Spring Security, puis génération du jeton. */
    @Transactional(readOnly = true)
    public AuthResponse connecter(ConnexionRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.motDePasse()));
        Utilisateur u = utilisateurRepository.findByEmail(email).orElseThrow();
        return reponse(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse profil(String email) {
        return reponse(utilisateurRepository.findByEmail(email).orElseThrow(), null);
    }

    private AuthResponse reponse(Utilisateur u) {
        return reponse(u, jwtService.genererToken(u.getEmail(), u.getRole().name()));
    }

    private AuthResponse reponse(Utilisateur u, String token) {
        return new AuthResponse(token, u.getId(), u.getEmail(), u.getNom(), u.getPrenom(), u.getRole().name());
    }
}
