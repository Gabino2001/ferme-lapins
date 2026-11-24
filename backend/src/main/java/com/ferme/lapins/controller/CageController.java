package com.ferme.lapins.controller;

import com.ferme.lapins.model.Cage;
import com.ferme.lapins.repository.CageRepo;
import com.ferme.lapins.repository.OwnedRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cages")
public class CageController extends CrudController<Cage> {
    @Autowired private CageRepo repo;
    @Override protected OwnedRepo<Cage> repo() { return repo; }
}
