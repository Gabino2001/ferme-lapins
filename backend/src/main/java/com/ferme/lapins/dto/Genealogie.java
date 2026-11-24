package com.ferme.lapins.dto;

import java.util.List;

public record Genealogie(Noeud ascendance, List<Enfant> enfants) {
    public record Noeud(Long id, String nom, String sexe, Noeud pere, Noeud mere) {}
    public record Enfant(Long id, String nom, String sexe) {}
}
