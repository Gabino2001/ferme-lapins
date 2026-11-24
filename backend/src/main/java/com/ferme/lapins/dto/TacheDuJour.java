package com.ferme.lapins.dto;

/** type : ROUTINE (tâche récurrente à cocher) ou ALERTE (problème à traiter). */
public record TacheDuJour(String type, Long id, String titre, String detail, String section) {}
