package com.ferme.lapins.dto;

import java.util.List;

/** severite : red | amb | blu — section : elevage | sante | cages | argent | lapins */
public record Alerte(String severite, String tag, String titre, String detail, List<String> solutions, String section) {}
