package fr.grandcinema.service;

import fr.grandcinema.domain.utilisateur.Utilisateur;
import fr.grandcinema.dto.auth.AuthResponse;
import fr.grandcinema.dto.auth.ConnexionRequest;
import fr.grandcinema.dto.auth.InscriptionRequest;
import fr.grandcinema.enumeration.Role;
import fr.grandcinema.exception.EmailDejaUtiliseException;
import fr.grandcinema.repository.utilisateur.UtilisateurRepository;
import fr.grandcinema.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Tests unitaires des US 1 et 2 : les dépendances sont simulées avec Mockito. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UtilisateurRepository utilisateurRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private final InscriptionRequest requete =
            new InscriptionRequest("Dupont", "Marie", "Marie@Exemple.fr", "Secret123!");

    @Test
    void inscription_chiffreLeMotDePasse_etAttribueLeRoleClient() {
        when(utilisateurRepository.existsByEmail("marie@exemple.fr")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hash-bcrypt");
        when(utilisateurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.genererToken("marie@exemple.fr", "CLIENT")).thenReturn("jeton");

        AuthResponse reponse = authService.inscrire(requete);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        Utilisateur enregistre = captor.getValue();
        assertThat(enregistre.getMotDePasse()).isEqualTo("hash-bcrypt");   // jamais en clair
        assertThat(enregistre.getEmail()).isEqualTo("marie@exemple.fr");   // normalisé
        assertThat(enregistre.getRole()).isEqualTo(Role.CLIENT);
        assertThat(reponse.token()).isEqualTo("jeton");
    }

    @Test
    void inscription_refuseUnEmailDejaUtilise() {
        when(utilisateurRepository.existsByEmail("marie@exemple.fr")).thenReturn(true);

        assertThatThrownBy(() -> authService.inscrire(requete))
                .isInstanceOf(EmailDejaUtiliseException.class);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void connexion_renvoieUnJeton_siLesIdentifiantsSontBons() {
        Utilisateur u = new Utilisateur();
        u.setEmail("marie@exemple.fr");
        u.setRole(Role.CLIENT);
        when(utilisateurRepository.findByEmail("marie@exemple.fr")).thenReturn(Optional.of(u));
        when(jwtService.genererToken("marie@exemple.fr", "CLIENT")).thenReturn("jeton");

        AuthResponse reponse = authService.connecter(new ConnexionRequest("marie@exemple.fr", "Secret123!"));

        assertThat(reponse.token()).isEqualTo("jeton");
    }

    @Test
    void connexion_echoue_siLeMotDePasseEstFaux() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("faux"));

        assertThatThrownBy(() -> authService.connecter(new ConnexionRequest("marie@exemple.fr", "mauvais")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).genererToken(any(), any());
    }
}
