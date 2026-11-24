package com.ferme.lapins.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

/** Tâche récurrente de la ferme (nourrir, nettoyer, peser...). */
@Entity
@Getter @Setter
public class Tache extends Proprio {
    private String titre;
    private Integer frequenceJours = 1;
    private LocalDate derniereRealisation;
}
