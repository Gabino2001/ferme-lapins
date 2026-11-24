package com.ferme.lapins.dto;

import java.time.LocalDate;
import java.util.List;

/** gainParJour et gainDernierePeriode en grammes par jour. */
public record Croissance(List<Point> points, Double gainParJour, Double gainDernierePeriode, String conseil) {
    public record Point(LocalDate date, double poids, Integer age) {}
}
