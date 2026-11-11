package fr.grandcinema.controller;

import fr.grandcinema.dto.reservation.PlanSalleResponse;
import fr.grandcinema.dto.reservation.ReservationRequest;
import fr.grandcinema.dto.reservation.ReservationResponse;
import fr.grandcinema.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Réservation", description = "Plan de salle, réservation, billet (US 16 à 20, 30, 31)")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/seances/{id}/plan")
    @Operation(summary = "Plan de salle en temps réel : places libres et occupées (US 16)")
    public PlanSalleResponse plan(@PathVariable Long id) {
        return reservationService.planDeSalle(id);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Réserver des places : blocage 10 minutes en attente de paiement (US 17 à 20)")
    @ApiResponse(responseCode = "201", description = "Places bloquées, récapitulatif renvoyé")
    @ApiResponse(responseCode = "400", description = "Plus de 10 places, ou place inexistante dans la salle")
    @ApiResponse(responseCode = "409", description = "Place déjà réservée par un autre client, ou séance commencée")
    public ReservationResponse reserver(@AuthenticationPrincipal UserDetails user,
                                        @Valid @RequestBody ReservationRequest request) {
        return reservationService.reserver(user.getUsername(), request);
    }

    @PostMapping("/reservations/{id}/confirmer")
    @Operation(summary = "Paiement simulé : confirme la réservation et génère le billet (US 30)")
    @ApiResponse(responseCode = "409", description = "Délai de 10 minutes dépassé ou réservation déjà traitée")
    public ReservationResponse confirmer(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return reservationService.confirmer(user.getUsername(), id);
    }

    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Abandonner une réservation en attente (libère les places)")
    public void annuler(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        reservationService.annuler(user.getUsername(), id);
    }

    @GetMapping("/reservations/mes")
    @Operation(summary = "Mes réservations (US 31)")
    public List<ReservationResponse> mesReservations(@AuthenticationPrincipal UserDetails user) {
        return reservationService.mesReservations(user.getUsername());
    }

    @GetMapping("/reservations/{id}")
    @Operation(summary = "Détail d'une réservation et code du billet (US 30)")
    public ReservationResponse detail(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return reservationService.detail(user.getUsername(), id);
    }
}
