package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.model.Utilisateur;
import com.ferme.lapins.repository.UtilisateurRepo;
import com.ferme.lapins.service.ParametresService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.NoSuchElementException;

/** Paramètres de la ferme. Lecture : tous les membres. Modification : propriétaire (voir SecurityConfig). */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {
    private final ParametresService parametres;
    private final UtilisateurRepo users;

    @GetMapping
    public Map<String, String> lire(@AuthenticationPrincipal Long uid) { return Map.of("devise", parametres.devise(uid)); }

    @PutMapping
    public Map<String, String> modifier(@AuthenticationPrincipal Long uid, @Valid @RequestBody Dtos.Devise r) {
        String d = r.devise().trim();
        if (d.isEmpty() || d.length() > 8) throw new IllegalArgumentException("Devise invalide (8 caractères maximum)");
        Utilisateur u = users.findById(uid).orElseThrow(() -> new NoSuchElementException("Ferme introuvable"));
        u.setDevise(d);
        users.save(u);
        return Map.of("devise", d);
    }
}
