package fr.grandcinema.controller;

import fr.grandcinema.dto.catalogue.FilmRequest;
import fr.grandcinema.dto.catalogue.FilmResponse;
import fr.grandcinema.dto.catalogue.SeanceRequest;
import fr.grandcinema.dto.catalogue.SeanceResponse;
import fr.grandcinema.service.ProgrammationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Accès réservé au rôle ADMIN (règle définie dans SecurityConfig sur /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Back-office", description = "Gestion des films et des séances (US 36 et 38) – rôle ADMIN")
public class AdminProgrammationController {

    private final ProgrammationService programmationService;

    public AdminProgrammationController(ProgrammationService programmationService) {
        this.programmationService = programmationService;
    }

    @GetMapping("/films")
    @Operation(summary = "Tous les films, archivés compris")
    public List<FilmResponse> films() {
        return programmationService.tousLesFilms();
    }

    @PostMapping("/films")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajouter un film (US 36)")
    public FilmResponse creerFilm(@Valid @RequestBody FilmRequest request) {
        return programmationService.creerFilm(request);
    }

    @PutMapping("/films/{id}")
    @Operation(summary = "Modifier un film (US 36)")
    public FilmResponse modifierFilm(@PathVariable Long id, @Valid @RequestBody FilmRequest request) {
        return programmationService.modifierFilm(id, request);
    }

    @DeleteMapping("/films/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archiver un film (US 36)")
    public void archiverFilm(@PathVariable Long id) {
        programmationService.archiverFilm(id);
    }

    @PostMapping("/seances")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Programmer une séance (US 38) – refusée si elle chevauche une autre séance")
    public SeanceResponse creerSeance(@Valid @RequestBody SeanceRequest request) {
        return programmationService.creerSeance(request);
    }
}
