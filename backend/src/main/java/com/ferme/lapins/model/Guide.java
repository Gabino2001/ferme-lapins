package com.ferme.lapins.model;

import java.util.List;

/** Guide des maladies : repères généraux, à confirmer avec un vétérinaire. */
public final class Guide {
    private Guide() {}
    public record Maladie(String nom, String signes, List<String> solutions) {}

    public static final List<Maladie> LISTE = List.of(
        new Maladie("Coccidiose", "Diarrhée, ventre ballonné, amaigrissement (jeunes de 4 à 12 semaines).",
            List.of("Isoler les malades", "Nettoyer et désinfecter cages et litière", "Traitement anticoccidien sur prescription vétérinaire", "Garder l’eau propre et l’élevage au sec")),
        new Maladie("Pasteurellose (coryza)", "Éternuements, écoulement nasal, respiration bruyante.",
            List.of("Isoler l’animal", "Aérer sans courant d’air", "Antibiotique uniquement sur prescription vétérinaire")),
        new Maladie("Entérite / diarrhée", "Selles molles, abattement, ventre gonflé.",
            List.of("Retirer les aliments frais et humides", "Foin à volonté et eau propre", "Vétérinaire si pas d’amélioration en 24 h")),
        new Maladie("Myxomatose / VHD", "Paupières gonflées, mort subite, saignements.",
            List.of("Pas de traitement efficace : isoler et prévenir le vétérinaire", "Vacciner tout le cheptel chaque année", "Lutter contre moustiques et puces")),
        new Maladie("Gale des oreilles", "Croûtes dans les oreilles, grattage, secouement de tête.",
            List.of("Traitement antiparasitaire vétérinaire", "Désinfecter cages et matériel", "Traiter tous les lapins en contact")),
        new Maladie("Mammite", "Mamelles rouges, dures et chaudes chez une lapine allaitante.",
            List.of("Isoler la lapine", "Consulter un vétérinaire rapidement", "Surveiller les lapereaux")),
        new Maladie("Coup de chaleur", "Bouche ouverte, oreilles rouges, prostration.",
            List.of("Mettre à l’ombre et ventiler", "Eau fraîche à volonté", "Bouteilles glacées près des cages")));

    public static List<String> solutions(String nom) {
        return LISTE.stream().filter(m -> m.nom().equals(nom)).findFirst()
                .map(Maladie::solutions).orElse(List.of("Consulter un vétérinaire"));
    }
}
