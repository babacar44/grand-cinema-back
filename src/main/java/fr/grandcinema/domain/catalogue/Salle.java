package fr.grandcinema.domain.catalogue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "salle")
@Getter
@Setter
@NoArgsConstructor
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_salle_code", nullable = false)
    private TypeSalle typeSalle;

    @Column(nullable = false)
    private Integer capacite;

    @Column(nullable = false)
    private boolean active = true;
}
