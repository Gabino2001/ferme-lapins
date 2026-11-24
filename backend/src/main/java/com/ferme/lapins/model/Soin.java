package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

/** Vaccin ou traitement planifié. lapinId = null : tout le cheptel. */
@Entity
@Getter @Setter
public class Soin extends Proprio {
    private String type;          // VACCIN | TRAITEMENT
    private String nom;
    private Long lapinId;
    private LocalDate datePrevue;
    private LocalDate dateRealisee;
    private Integer rappelJours;  // si renseigné : le prochain soin est planifié automatiquement
    @Column(length = 1000) private String notes;
}
