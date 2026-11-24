package com.ferme.lapins.controller;

import com.ferme.lapins.model.PhotoLapin;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.repository.PhotoLapinRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/lapins/{id}/photo")
@RequiredArgsConstructor
public class PhotoController {
    private static final int TAILLE_MAX = 1_500_000; // caractères base64 (~1,1 Mo)
    private final LapinRepo lapins;
    private final PhotoLapinRepo photos;

    private void verifier(Long uid, Long id) {
        lapins.findById(id).filter(l -> uid.equals(l.getUtilisateurId()))
                .orElseThrow(() -> new NoSuchElementException("Lapin introuvable"));
    }

    @GetMapping
    public Map<String, Object> lire(@AuthenticationPrincipal Long uid, @PathVariable Long id) {
        verifier(uid, id);
        Map<String, Object> m = new HashMap<>();
        m.put("photo", photos.findByLapinIdAndUtilisateurId(id, uid).map(PhotoLapin::getDonnees).orElse(null));
        return m;
    }

    @PutMapping
    public Map<String, String> enregistrer(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody Map<String, String> body) {
        verifier(uid, id);
        String donnees = body.get("photo");
        if (donnees == null || donnees.isBlank()) throw new IllegalArgumentException("Photo vide");
        if (donnees.length() > TAILLE_MAX) throw new IllegalArgumentException("Photo trop lourde");
        PhotoLapin p = photos.findByLapinIdAndUtilisateurId(id, uid).orElseGet(PhotoLapin::new);
        p.setUtilisateurId(uid);
        p.setLapinId(id);
        p.setDonnees(donnees);
        photos.save(p);
        return Map.of("message", "Photo enregistrée");
    }

    @DeleteMapping
    public void supprimer(@AuthenticationPrincipal Long uid, @PathVariable Long id) {
        verifier(uid, id);
        photos.findByLapinIdAndUtilisateurId(id, uid).ifPresent(photos::delete);
    }
}
