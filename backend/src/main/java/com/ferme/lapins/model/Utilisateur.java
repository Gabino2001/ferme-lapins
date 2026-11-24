package com.ferme.lapins.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Utilisateur {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    @Column(unique = true, nullable = false)
    private String email;
    @JsonIgnore
    private String motDePasse;
    /** null : propriétaire de la ferme ; sinon id du propriétaire dont la personne est employée. */
    private Long proprietaireId;
    /** PROPRIETAIRE | OUVRIER | LECTEUR */
    private String role = "PROPRIETAIRE";
    /** Devise d'affichage des montants (réglage de la ferme, porté par le propriétaire). */
    private String devise = "FCFA";
}
