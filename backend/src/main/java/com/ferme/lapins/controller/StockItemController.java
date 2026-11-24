package com.ferme.lapins.controller;

import com.ferme.lapins.model.StockItem;
import com.ferme.lapins.repository.OwnedRepo;
import com.ferme.lapins.repository.StockItemRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
public class StockItemController extends CrudController<StockItem> {
    @Autowired private StockItemRepo repo;
    @Override protected OwnedRepo<StockItem> repo() { return repo; }
}
