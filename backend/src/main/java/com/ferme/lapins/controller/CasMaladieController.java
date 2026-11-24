package com.ferme.lapins.controller;

import com.ferme.lapins.model.CasMaladie;
import com.ferme.lapins.repository.CasMaladieRepo;
import com.ferme.lapins.repository.OwnedRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maladies")
public class CasMaladieController extends CrudController<CasMaladie> {
    @Autowired private CasMaladieRepo repo;
    @Override protected OwnedRepo<CasMaladie> repo() { return repo; }
}
