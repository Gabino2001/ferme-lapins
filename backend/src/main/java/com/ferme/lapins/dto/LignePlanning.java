package com.ferme.lapins.dto;

import java.time.LocalDate;

/** etat : PRETE | GESTANTE | ALLAITANTE | REPOS | TROP_JEUNE */
public record LignePlanning(Long femelleId, String nom, String cage, String etat, String detail, LocalDate dateCle,
                            Long maleId, String maleNom, String niveauParente, String conseil) {}
