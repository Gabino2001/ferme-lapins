package com.ferme.lapins.service;

import com.ferme.lapins.event.FermeChanged;
import com.ferme.lapins.model.Soin;
import com.ferme.lapins.repository.SoinRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class SoinService {
    private final SoinRepo soins;
    private final ApplicationEventPublisher events;

    /** Marque le soin comme fait et planifie le suivant si un rappel est défini. */
    public Soin realise(Long uid, Long id, LocalDate date) {
        Soin s = soins.findById(id).filter(x -> uid.equals(x.getUtilisateurId()))
                .orElseThrow(() -> new NoSuchElementException("Introuvable"));
        if (s.getDateRealisee() != null) throw new IllegalArgumentException("Soin déjà enregistré");
        LocalDate d = date == null ? LocalDate.now() : date;
        s.setDateRealisee(d);
        soins.save(s);
        if (s.getRappelJours() != null && s.getRappelJours() > 0) {
            Soin suite = new Soin();
            suite.setUtilisateurId(uid);
            suite.setType(s.getType());
            suite.setNom(s.getNom());
            suite.setLapinId(s.getLapinId());
            suite.setRappelJours(s.getRappelJours());
            suite.setDatePrevue(d.plusDays(s.getRappelJours()));
            soins.save(suite);
        }
        events.publishEvent(new FermeChanged(uid));
        return s;
    }
}
