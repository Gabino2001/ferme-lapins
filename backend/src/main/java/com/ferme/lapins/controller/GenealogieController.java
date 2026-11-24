package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Genealogie;
import com.ferme.lapins.dto.Parente;
import com.ferme.lapins.service.GenealogieService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/genealogie")
@RequiredArgsConstructor
public class GenealogieController {
    private final GenealogieService service;

    @GetMapping("/parente")
    public Parente parente(@AuthenticationPrincipal Long uid, @RequestParam Long femelleId, @RequestParam Long maleId) {
        return service.analyser(uid, femelleId, maleId);
    }

    @GetMapping("/{id}")
    public Genealogie arbre(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return service.arbre(uid, id); }
}
