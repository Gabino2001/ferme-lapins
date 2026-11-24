package com.ferme.lapins.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class Commande extends Proprio {
    private Long clientId;
    private String produit;                 // ex. Lapin pour la viande
    private Integer quantite;
    private Double prixUnitaire;
    private Double montantPaye = 0.0;
    private LocalDate dateCommande;
    private LocalDate dateLivraisonPrevue;
    private LocalDate dateLivraison;
    private String statut = "EN_ATTENTE";   // EN_ATTENTE | LIVREE | ANNULEE
    @Column(length = 1000) private String notes;
}
