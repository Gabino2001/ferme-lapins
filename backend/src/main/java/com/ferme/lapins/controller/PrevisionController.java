package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Previsions;
import com.ferme.lapins.service.PrevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/previsions")
@RequiredArgsConstructor
public class PrevisionController {
    private final PrevisionService service;

    @GetMapping
    public Previsions previsions(@AuthenticationPrincipal Long uid) { return service.calculer(uid); }
}
