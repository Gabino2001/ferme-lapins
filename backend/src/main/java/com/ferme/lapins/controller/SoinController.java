package com.ferme.lapins.controller;

import com.ferme.lapins.model.Soin;
import com.ferme.lapins.repository.OwnedRepo;
import com.ferme.lapins.repository.SoinRepo;
import com.ferme.lapins.service.SoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/soins")
public class SoinController extends CrudController<Soin> {
    @Autowired private SoinRepo repo;
    @Autowired private SoinService service;

    @Override protected OwnedRepo<Soin> repo() { return repo; }

    @PostMapping("/{id}/realise")
    public Soin realise(@AuthenticationPrincipal Long uid, @PathVariable Long id,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.realise(uid, id, date);
    }
}
