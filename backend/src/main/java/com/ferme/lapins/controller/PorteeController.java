package com.ferme.lapins.controller;

import com.ferme.lapins.model.Portee;
import com.ferme.lapins.repository.OwnedRepo;
import com.ferme.lapins.repository.PorteeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portees")
public class PorteeController extends CrudController<Portee> {
    @Autowired private PorteeRepo repo;
    @Override protected OwnedRepo<Portee> repo() { return repo; }
}
