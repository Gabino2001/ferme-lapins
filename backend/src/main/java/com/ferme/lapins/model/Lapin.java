package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class Lapin extends Proprio {
    private String nom, sexe, race, cage, pere, mere, statut;
    private Long pereId, mereId; // parents présents dans l’élevage (pere / mere : nom libre pour un parent extérieur)
    @Column(length = 2000) private String notes;
    private LocalDate dateNaissance;
    private Double poids;
}
