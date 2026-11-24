package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class StockItem extends Proprio {
    private String nom, unite; private Double quantite = 0.0, consoJour = 0.0;
}
