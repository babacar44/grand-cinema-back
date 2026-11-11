package fr.grandcinema.domain.catalogue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "place")
@Getter
@Setter
@NoArgsConstructor
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @Column(nullable = false, length = 2)
    private String rangee;

    @Column(nullable = false)
    private Integer numero;

    /** Place adaptée aux personnes à mobilité réduite. */
    @Column(nullable = false)
    private boolean pmr;
}
