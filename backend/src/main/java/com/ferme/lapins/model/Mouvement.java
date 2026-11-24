package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class Mouvement extends Proprio {
    private String type, categorie; private Integer quantite; private Double montant = 0.0; private LocalDate date;
}
