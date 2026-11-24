package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class CasMaladie extends Proprio {
    private Long lapinId; private String maladie, statut = "EN_TRAITEMENT"; private LocalDate date;
}
