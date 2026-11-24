package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.model.Utilisateur;
import com.ferme.lapins.repository.UtilisateurRepo;
import com.ferme.lapins.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final int ESSAIS_MAX = 10;
    private static final Duration FENETRE = Duration.ofMinutes(15);

    private final UtilisateurRepo users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Map<String, Deque<Instant>> echecs = new ConcurrentHashMap<>();

    /** Crée une nouvelle ferme dont la personne est propriétaire. */
    @PostMapping("/register")
    public Dtos.AuthResponse register(@Valid @RequestBody Dtos.Register r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new IllegalArgumentException("Cet email est déjà utilisé");
        Utilisateur u = new Utilisateur();
        u.setNom(r.nom().trim());
        u.setEmail(email);
        u.setMotDePasse(encoder.encode(r.motDePasse()));
        u.setRole("PROPRIETAIRE");
        users.save(u);
        return new Dtos.AuthResponse(jwt.generer(u.getId(), email), u.getNom(), email, "PROPRIETAIRE");
    }

    @PostMapping("/login")
    public Dtos.AuthResponse login(@RequestBody Dtos.Login r) {
        String email = r.email() == null ? "" : r.email().trim().toLowerCase();
        verifierEssais(email);
        Utilisateur u = users.findByEmail(email)
                .filter(x -> encoder.matches(r.motDePasse() == null ? "" : r.motDePasse(), x.getMotDePasse()))
                .orElse(null);
        if (u == null) {
            enregistrerEchec(email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        }
        echecs.remove(email);
        String role = u.getRole() == null ? "PROPRIETAIRE" : u.getRole();
        return new Dtos.AuthResponse(jwt.generer(u.getId(), u.getEmail()), u.getNom(), u.getEmail(), role);
    }

    /** Protection contre les essais répétés : 10 échecs en 15 minutes bloquent la connexion pour cet email. */
    private void verifierEssais(String email) {
        Deque<Instant> d = echecs.get(email);
        if (d == null) return;
        synchronized (d) {
            Instant seuil = Instant.now().minus(FENETRE);
            while (!d.isEmpty() && d.peekFirst().isBefore(seuil)) d.pollFirst();
            if (d.size() >= ESSAIS_MAX) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop d’essais : réessayez dans quelques minutes");
            }
        }
    }

    private void enregistrerEchec(String email) {
        Deque<Instant> d = echecs.computeIfAbsent(email, k -> new ArrayDeque<>());
        synchronized (d) { d.addLast(Instant.now()); }
    }
}
