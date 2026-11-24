package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.model.Utilisateur;
import com.ferme.lapins.repository.UtilisateurRepo;
import com.ferme.lapins.security.Auth;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/compte")
@RequiredArgsConstructor
public class CompteController {
    private final UtilisateurRepo users;
    private final PasswordEncoder encoder;

    @PostMapping("/mot-de-passe")
    public Map<String, String> changer(@AuthenticationPrincipal Long uid, @Valid @RequestBody Dtos.ChangerMotDePasse r) {
        Utilisateur u = users.findById(Auth.utilisateurId() != null ? Auth.utilisateurId() : uid).orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable"));
        if (!encoder.matches(r.ancien(), u.getMotDePasse())) throw new IllegalArgumentException("Ancien mot de passe incorrect");
        u.setMotDePasse(encoder.encode(r.nouveau()));
        users.save(u);
        return Map.of("message", "Mot de passe modifié");
    }
}
