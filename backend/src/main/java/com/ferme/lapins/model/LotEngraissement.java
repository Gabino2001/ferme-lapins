package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class LotEngraissement extends Proprio {
    private Long mereId; private Integer nombre; private String cage; private LocalDate dateNaissance;
}
