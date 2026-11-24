package com.ferme.lapins.dto;

import java.util.List;
import java.util.Map;

public record Dashboard(int score, String statut, String resume, Map<String, Integer> kpis, List<Alerte> alertes) {}
