package fr.grandcinema.domain.catalogue;

import fr.grandcinema.catalogue.VersionFilm;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "seance")
@Getter
@Setter
@NoArgsConstructor
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @Column(nullable = false)
    private LocalDateTime debut;

    /** Fin = début + durée du film + battement de nettoyage. */
    @Column(nullable = false)
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VersionFilm version = VersionFilm.VF;
}
