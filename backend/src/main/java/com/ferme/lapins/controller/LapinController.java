package com.ferme.lapins.controller;

import com.ferme.lapins.model.Lapin;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.repository.OwnedRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lapins")
public class LapinController extends CrudController<Lapin> {
    @Autowired private LapinRepo repo;
    @Override protected OwnedRepo<Lapin> repo() { return repo; }
}
