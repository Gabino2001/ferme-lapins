package com.ferme.lapins.controller;

import com.ferme.lapins.model.Client;
import com.ferme.lapins.repository.ClientRepo;
import com.ferme.lapins.repository.OwnedRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController extends CrudController<Client> {
    @Autowired private ClientRepo repo;
    @Override protected OwnedRepo<Client> repo() { return repo; }
}
