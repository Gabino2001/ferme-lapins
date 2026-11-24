package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class Pesee extends Proprio {
    private Long lapinId; private LocalDate date; private Double poids;
}
