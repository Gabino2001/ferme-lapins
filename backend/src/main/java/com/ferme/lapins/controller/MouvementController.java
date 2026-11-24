package com.ferme.lapins.controller;

import com.ferme.lapins.model.Mouvement;
import com.ferme.lapins.repository.MouvementRepo;
import com.ferme.lapins.repository.OwnedRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mouvements")
public class MouvementController extends CrudController<Mouvement> {
    @Autowired private MouvementRepo repo;
    @Override protected OwnedRepo<Mouvement> repo() { return repo; }
}
