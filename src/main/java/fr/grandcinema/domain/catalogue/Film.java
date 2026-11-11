package fr.grandcinema.domain.catalogue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "film")
@Getter
@Setter
@NoArgsConstructor
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "duree_minutes", nullable = false)
    private Integer dureeMinutes;

    @Column(length = 50)
    private String genre;

    @Column(length = 150)
    private String realisateur;

    @Column(name = "age_minimum", nullable = false)
    private Integer ageMinimum = 0;

    @Column(name = "affiche_url", length = 500)
    private String afficheUrl;

    /** Un film n'est jamais supprimé, il est archivé (actif = false). */
    @Column(nullable = false)
    private boolean actif = true;
}
