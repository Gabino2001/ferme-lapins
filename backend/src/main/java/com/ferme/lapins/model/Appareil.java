package com.ferme.lapins.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/** Téléphone enregistré pour les notifications push (jeton Firebase). */
@Entity
@Getter @Setter
public class Appareil extends Proprio {
    @Column(length = 512, unique = true)
    private String token;
    private String plateforme;
}
