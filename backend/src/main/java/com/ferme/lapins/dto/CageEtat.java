package com.ferme.lapins.dto;

import java.util.List;

/** État d’une cage sur le plan de la ferme. etat : red | amb | ok | free */
public record CageEtat(
        Long id, String numero, String type, int capacite, int occupation,
        String etat, String libelle, List<String> occupants,
        boolean surpeuplee, boolean melange, boolean malade) {}
