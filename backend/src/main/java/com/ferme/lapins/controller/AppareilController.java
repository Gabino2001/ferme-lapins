package com.ferme.lapins.controller;

import com.ferme.lapins.model.Appareil;
import com.ferme.lapins.repository.AppareilRepo;
import com.ferme.lapins.service.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/appareils")
@RequiredArgsConstructor
public class AppareilController {
    private final AppareilRepo appareils;
    private final PushService push;

    public record Enregistrement(String token, String plateforme) {}

    /** Enregistre (ou rattache à l'utilisateur courant) le jeton Firebase d'un téléphone. */
    @PostMapping
    public Map<String, Object> enregistrer(@AuthenticationPrincipal Long uid, @RequestBody Enregistrement r) {
        if (r.token() == null || r.token().isBlank()) throw new IllegalArgumentException("Jeton manquant");
        Appareil a = appareils.findByToken(r.token()).orElseGet(Appareil::new);
        a.setToken(r.token());
        a.setPlateforme(r.plateforme());
        a.setUtilisateurId(uid);
        appareils.save(a);
        Map<String, Object> m = new HashMap<>();
        m.put("pushActif", push.actif());
        return m;
    }

    /** Envoie une notification de test sur les téléphones de l'utilisateur. */
    @PostMapping("/test")
    public Map<String, Object> test(@AuthenticationPrincipal Long uid) {
        int envoyees = 0;
        for (Appareil a : appareils.findByUtilisateurId(uid)) {
            if (push.envoyer(a.getToken(), "Ferme des Lapins", "Notification de test : tout fonctionne !")) envoyees++;
            else appareils.delete(a);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("pushActif", push.actif());
        m.put("envoyees", envoyees);
        return m;
    }
}
