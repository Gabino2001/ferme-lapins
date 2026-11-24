package com.ferme.lapins.controller;

import com.ferme.lapins.model.Saillie;
import com.ferme.lapins.repository.OwnedRepo;
import com.ferme.lapins.repository.SaillieRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/saillies")
public class SaillieController extends CrudController<Saillie> {
    @Autowired private SaillieRepo repo;
    @Override protected OwnedRepo<Saillie> repo() { return repo; }
}
