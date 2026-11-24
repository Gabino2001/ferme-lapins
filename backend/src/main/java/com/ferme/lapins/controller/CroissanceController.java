package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Croissance;
import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.service.CroissanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lots/{id}/pesees")
@RequiredArgsConstructor
public class CroissanceController {
    private final CroissanceService service;

    @GetMapping
    public Croissance courbe(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return service.courbe(uid, id); }

    /** poids = poids moyen d'un lapin du lot, en kg. */
    @PostMapping
    public Croissance peser(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody Dtos.PeseeReq d) {
        return service.peser(uid, id, d.poids(), d.date());
    }
}
