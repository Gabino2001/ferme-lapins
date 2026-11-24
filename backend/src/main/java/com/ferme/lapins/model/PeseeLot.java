package com.ferme.lapins.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

/** Pesée d'un lot d'engraissement : poids moyen d'un lapin, en kg. */
@Entity
@Getter @Setter
public class PeseeLot extends Proprio {
    private Long lotId;
    private LocalDate date;
    private Double poidsMoyen;
}
