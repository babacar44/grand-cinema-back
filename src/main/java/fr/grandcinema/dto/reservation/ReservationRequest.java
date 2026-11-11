package fr.grandcinema.dto.reservation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** US 17 – Places choisies sur le plan de salle (10 places maximum par réservation). */
public record ReservationRequest(
        @NotNull Long seanceId,
        @NotEmpty @Size(max = 10, message = "10 places maximum par réservation") List<@NotNull Long> placeIds
) {
}
