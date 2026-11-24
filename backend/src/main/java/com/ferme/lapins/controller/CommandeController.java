package com.ferme.lapins.controller;

import com.ferme.lapins.model.Commande;
import com.ferme.lapins.repository.CommandeRepo;
import com.ferme.lapins.repository.OwnedRepo;
import com.ferme.lapins.service.CommandeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController extends CrudController<Commande> {
    @Autowired private CommandeRepo repo;
    @Autowired private CommandeService service;

    public record Livraison(Long lotId, LocalDate date) {}

    @Override protected OwnedRepo<Commande> repo() { return repo; }

    @PostMapping("/{id}/livrer")
    public Commande livrer(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody(required = false) Livraison l) {
        return service.livrer(uid, id, l == null ? null : l.lotId(), l == null ? null : l.date());
    }

    @PostMapping("/{id}/paiement")
    public Commande paiement(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestParam double montant) {
        return service.paiement(uid, id, montant);
    }

    @PostMapping("/{id}/annuler")
    public Commande annuler(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return service.annuler(uid, id); }
}
