package com.ferme.lapins.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class Portee extends Proprio {
    private Long mereId; private LocalDate date; private Integer nes, vivants, sevres; private Boolean sevree = false;
}
