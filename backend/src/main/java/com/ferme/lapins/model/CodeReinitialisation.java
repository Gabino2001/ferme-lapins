package com.ferme.lapins.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** Code à 6 chiffres envoyé par email pour réinitialiser un mot de passe (stocké haché, valable 15 minutes). */
@Entity
@Getter @Setter
public class CodeReinitialisation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long utilisateurId;
    private String codeHash;
    private LocalDateTime creeLe;
    private LocalDateTime expiration;
    private int tentatives;
}
