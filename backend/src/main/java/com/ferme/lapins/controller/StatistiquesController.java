package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Statistiques;
import com.ferme.lapins.service.StatistiquesService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistiques")
@RequiredArgsConstructor
public class StatistiquesController {
    private final StatistiquesService service;

    @GetMapping
    public Statistiques statistiques(@AuthenticationPrincipal Long uid) { return service.calculer(uid); }
}
