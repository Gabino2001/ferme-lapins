package com.ferme.lapins.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

/** Photo d'un lapin (image réduite, encodée en base64). */
@Entity
@Getter @Setter
public class PhotoLapin extends Proprio {
    private Long lapinId;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String donnees;
}
