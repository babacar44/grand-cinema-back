package fr.grandcinema.controller;

import fr.grandcinema.dto.auth.AuthResponse;
import fr.grandcinema.dto.auth.ConnexionRequest;
import fr.grandcinema.dto.auth.InscriptionRequest;
import fr.grandcinema.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Inscription, connexion et profil (US 1 à 4)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte client (US 1)")
    public AuthResponse inscrire(@Valid @RequestBody InscriptionRequest request) {
        return authService.inscrire(request);
    }

    @PostMapping("/connexion")
    @Operation(summary = "Se connecter et obtenir un jeton JWT (US 2)")
    public AuthResponse connecter(@Valid @RequestBody ConnexionRequest request) {
        return authService.connecter(request);
    }

    @GetMapping("/moi")
    @Operation(summary = "Profil de l'utilisateur connecté")
    public AuthResponse moi(@AuthenticationPrincipal UserDetails user) {
        return authService.profil(user.getUsername());
    }
}
