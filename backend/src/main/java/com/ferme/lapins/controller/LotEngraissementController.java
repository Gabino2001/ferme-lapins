package com.ferme.lapins.controller;

import com.ferme.lapins.model.LotEngraissement;
import com.ferme.lapins.repository.LotEngraissementRepo;
import com.ferme.lapins.repository.OwnedRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lots")
public class LotEngraissementController extends CrudController<LotEngraissement> {
    @Autowired private LotEngraissementRepo repo;
    @Override protected OwnedRepo<LotEngraissement> repo() { return repo; }
}
