package fr.grandcinema.domain.catalogue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Type de salle (Premium/IMAX, Standard, VIP, Événementielle) et son tarif (US 18). */
@Entity
@Table(name = "type_salle")
@Getter
@Setter
@NoArgsConstructor
public class TypeSalle {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String libelle;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal prix;
}
