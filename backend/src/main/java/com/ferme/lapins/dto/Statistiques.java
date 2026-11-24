package com.ferme.lapins.dto;

import java.util.List;

public record Statistiques(
        int nbPortees, double tailleMoyennePortee, double vivantsMoyens, double mortaliteSevrage,
        int lapinsVendus90j, double coutAlimentParLapin,
        List<LapineStat> classement, List<MoisStat> mensuel) {

    public record LapineStat(Long id, String nom, int portees, int nes, int sevres, double moyenneNes) {}

    public record MoisStat(String mois, double ventes, double depenses, double resultat) {}
}
