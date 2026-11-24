package com.ferme.lapins.controller;

import com.ferme.lapins.dto.TacheDuJour;
import com.ferme.lapins.model.Tache;
import com.ferme.lapins.repository.OwnedRepo;
import com.ferme.lapins.repository.TacheRepo;
import com.ferme.lapins.service.TacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/taches")
public class TacheController extends CrudController<Tache> {
    @Autowired private TacheRepo repo;
    @Autowired private TacheService service;

    @Override protected OwnedRepo<Tache> repo() { return repo; }

    @GetMapping("/aujourdhui")
    public List<TacheDuJour> aujourdhui(@AuthenticationPrincipal Long uid) { return service.aujourdhui(uid); }

    @PostMapping("/{id}/faite")
    public Tache faite(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return service.faite(uid, id); }

    @PostMapping("/defauts")
    public List<Tache> defauts(@AuthenticationPrincipal Long uid) { return service.defauts(uid); }
}
