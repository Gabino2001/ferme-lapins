package com.ferme.lapins.dto;

import java.util.Map;

public record Bilan(
        double ventes, double depenses, double resultat, int lapinsVendus,
        double coutRevient, double prixMoyen, Map<String, Double> depensesParPoste) {}
