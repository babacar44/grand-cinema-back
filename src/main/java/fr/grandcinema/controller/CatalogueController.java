package fr.grandcinema.controller;

import fr.grandcinema.dto.catalogue.FilmDetailResponse;
import fr.grandcinema.dto.catalogue.FilmResponse;
import fr.grandcinema.dto.catalogue.SeanceResponse;
import fr.grandcinema.service.CatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Catalogue", description = "Consultation publique des films et des séances (US 9 à 11)")
public class CatalogueController {

    private final CatalogueService catalogueService;

    public CatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @GetMapping("/films")
    @Operation(summary = "Films à l'affiche (US 9)")
    public List<FilmResponse> filmsALaffiche() {
        return catalogueService.filmsALaffiche();
    }

    @GetMapping("/films/{id}")
    @Operation(summary = "Fiche d'un film avec ses prochaines séances (US 10)")
    public FilmDetailResponse ficheFilm(@PathVariable Long id) {
        return catalogueService.ficheFilm(id);
    }

    @GetMapping("/films/{id}/seances")
    @Operation(summary = "Séances d'un film sur 7 jours (US 11)")
    public List<SeanceResponse> seances(@PathVariable Long id) {
        return catalogueService.seancesDuFilm(id);
    }

    @GetMapping("/seances/{id}")
    @Operation(summary = "Détail d'une séance")
    public SeanceResponse seance(@PathVariable Long id) {
        return catalogueService.seance(id);
    }
}
