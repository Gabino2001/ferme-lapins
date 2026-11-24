package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.model.Utilisateur;
import com.ferme.lapins.repository.UtilisateurRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;

/** Gestion de l'équipe (réservée au propriétaire). Rôles : OUVRIER (tout sauf l'argent) et LECTEUR (consultation seule). */
@RestController
@RequestMapping("/api/equipe")
@RequiredArgsConstructor
public class EquipeController {
    private static final List<String> ROLES = List.of("OUVRIER", "LECTEUR");
    private final UtilisateurRepo users;
    private final PasswordEncoder encoder;

    public record Membre(Long id, String nom, String email, String role) {}

    private Membre vers(Utilisateur u) { return new Membre(u.getId(), u.getNom(), u.getEmail(), u.getRole()); }

    private Utilisateur membre(Long uid, Long id) {
        return users.findById(id).filter(u -> uid.equals(u.getProprietaireId()))
                .orElseThrow(() -> new NoSuchElementException("Membre introuvable"));
    }

    @GetMapping
    public List<Membre> liste(@AuthenticationPrincipal Long uid) {
        return users.findByProprietaireId(uid).stream().map(this::vers).toList();
    }

    @PostMapping
    public Membre ajouter(@AuthenticationPrincipal Long uid, @Valid @RequestBody Dtos.NouveauMembre r) {
        if (!ROLES.contains(r.role())) throw new IllegalArgumentException("Rôle invalide (OUVRIER ou LECTEUR)");
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new IllegalArgumentException("Cet email est déjà utilisé");
        Utilisateur u = new Utilisateur();
        u.setNom(r.nom().trim());
        u.setEmail(email);
        u.setMotDePasse(encoder.encode(r.motDePasse()));
        u.setProprietaireId(uid);
        u.setRole(r.role());
        return vers(users.save(u));
    }

    @PutMapping("/{id}/role")
    public Membre changerRole(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestParam String role) {
        if (!ROLES.contains(role)) throw new IllegalArgumentException("Rôle invalide (OUVRIER ou LECTEUR)");
        Utilisateur u = membre(uid, id);
        u.setRole(role);
        return vers(users.save(u));
    }

    /** Le propriétaire définit un nouveau mot de passe pour un membre de l'équipe. */
    @PutMapping("/{id}/mot-de-passe")
    public void reinitialiserMotDePasse(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody Dtos.NouveauMdp r) {
        Utilisateur u = membre(uid, id);
        u.setMotDePasse(encoder.encode(r.nouveau()));
        users.save(u);
    }

    @DeleteMapping("/{id}")
    public void retirer(@AuthenticationPrincipal Long uid, @PathVariable Long id) {
        users.delete(membre(uid, id));
    }
}
