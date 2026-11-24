package com.ferme.lapins.dto;

import java.time.LocalDate;
import java.util.List;

/** Prévisions sur 30 jours. revenuEstime et commandesEnAttente sont null pour les non-propriétaires. */
public record Previsions(List<Evenement> evenements, int lapereauxAttendus, int lapinsPretsAVendre,
                         List<Rupture> ruptures, Double revenuEstime, Double commandesEnAttente) {

    /** type : MISE_BAS | SEVRAGE | VENTE | SOIN | LIVRAISON */
    public record Evenement(String type, String titre, LocalDate date, String detail) {}

    public record Rupture(String nom, LocalDate date, int jours) {}
}
