package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class Saillie extends Proprio {
    private Long femelleId, maleId; private LocalDate date; private String palpation;
}
