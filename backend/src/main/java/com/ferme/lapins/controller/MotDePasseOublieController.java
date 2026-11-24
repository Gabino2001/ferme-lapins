package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.service.ReinitialisationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MotDePasseOublieController {
    private final ReinitialisationService service;

    @PostMapping("/mot-de-passe-oublie")
    public Map<String, String> demander(@RequestBody Dtos.DemandeCode r) {
        service.demander(r.email());
        return Map.of("message", "Si ce compte existe, un code vient d’être envoyé par email.");
    }

    @PostMapping("/reinitialiser")
    public Map<String, String> reinitialiser(@RequestBody Dtos.Reinitialisation r) {
        service.reinitialiser(r.email(), r.code(), r.nouveau());
        return Map.of("message", "Mot de passe modifié : vous pouvez vous connecter.");
    }
}
