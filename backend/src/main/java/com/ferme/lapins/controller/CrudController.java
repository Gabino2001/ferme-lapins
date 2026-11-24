package com.ferme.lapins.controller;

import com.ferme.lapins.event.FermeChanged;
import com.ferme.lapins.model.Proprio;
import com.ferme.lapins.repository.OwnedRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;

/** CRUD générique : chaque utilisateur ne voit et ne modifie que ses propres données. */
public abstract class CrudController<T extends Proprio> {
    @Autowired protected ApplicationEventPublisher events;

    protected abstract OwnedRepo<T> repo();

    @GetMapping
    public List<T> list(@AuthenticationPrincipal Long uid) {
        return repo().findByUtilisateurId(uid);
    }

    @PostMapping
    public T create(@AuthenticationPrincipal Long uid, @RequestBody T body) {
        body.setId(null);
        body.setUtilisateurId(uid);
        T saved = repo().save(body);
        events.publishEvent(new FermeChanged(uid));
        return saved;
    }

    @PutMapping("/{id}")
    public T update(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody T body) {
        own(uid, id);
        body.setId(id);
        body.setUtilisateurId(uid);
        T saved = repo().save(body);
        events.publishEvent(new FermeChanged(uid));
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) {
        repo().delete(own(uid, id));
        events.publishEvent(new FermeChanged(uid));
    }

    private T own(Long uid, Long id) {
        return repo().findById(id).filter(x -> uid.equals(x.getUtilisateurId()))
                .orElseThrow(() -> new NoSuchElementException("Introuvable"));
    }
}
