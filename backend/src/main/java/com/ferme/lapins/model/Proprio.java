package com.ferme.lapins.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Toute donnée de ferme appartient à un utilisateur (multi-tenant). */
@MappedSuperclass
@Getter @Setter
public abstract class Proprio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;
}
