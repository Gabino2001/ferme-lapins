package com.ferme.lapins.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Client extends Proprio {
    private String nom, telephone, adresse;
    @Column(length = 1000) private String notes;
}
