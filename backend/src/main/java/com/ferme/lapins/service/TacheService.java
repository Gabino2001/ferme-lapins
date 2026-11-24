package com.ferme.lapins.service;

import com.ferme.lapins.dto.Alerte;
import com.ferme.lapins.dto.TacheDuJour;
import com.ferme.lapins.event.FermeChanged;
import com.ferme.lapins.model.Tache;
import com.ferme.lapins.repository.TacheRepo;
import com.ferme.lapins.security.Auth;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import static com.ferme.lapins.util.Util.nz;

@Service
@RequiredArgsConstructor
@Transactional
public class TacheService {
    private final TacheRepo taches;
    private final AlerteService alertes;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<TacheDuJour> aujourdhui(Long uid) {
        LocalDate today = LocalDate.now();
        List<TacheDuJour> out = new ArrayList<>();
        for (Alerte a : alertes.calculer(uid)) {
            if (!"blu".equals(a.severite()) && (Auth.estProprietaire() || !List.of("argent", "clients").contains(a.section()))) out.add(new TacheDuJour("ALERTE", null, a.titre(), a.detail(), a.section()));
        }
        for (Tache t : taches.findByUtilisateurId(uid)) {
            int f = Math.max(1, nz(t.getFrequenceJours()));
            boolean due = t.getDerniereRealisation() == null || !t.getDerniereRealisation().plusDays(f).isAfter(today);
            if (due) out.add(new TacheDuJour("ROUTINE", t.getId(), t.getTitre(), f == 1 ? "Chaque jour" : "Tous les " + f + " jours", null));
        }
        return out;
    }

    public Tache faite(Long uid, Long id) {
        Tache t = taches.findById(id).filter(x -> uid.equals(x.getUtilisateurId()))
                .orElseThrow(() -> new NoSuchElementException("Introuvable"));
        t.setDerniereRealisation(LocalDate.now());
        taches.save(t);
        events.publishEvent(new FermeChanged(uid));
        return t;
    }

    /** Crée les tâches habituelles si l'utilisateur n'en a encore aucune. */
    public List<Tache> defauts(Long uid) {
        List<Tache> existantes = taches.findByUtilisateurId(uid);
        if (!existantes.isEmpty()) return existantes;
        String[][] modele = {
                {"Nourrir et abreuver les lapins", "1"},
                {"Vérifier l’eau et les abreuvoirs", "1"},
                {"Nettoyer les cages et changer la litière", "7"},
                {"Peser les lapereaux en engraissement", "7"},
                {"Désinfecter le matériel", "30"}};
        List<Tache> creees = new ArrayList<>();
        for (String[] m : modele) {
            Tache t = new Tache();
            t.setUtilisateurId(uid);
            t.setTitre(m[0]);
            t.setFrequenceJours(Integer.parseInt(m[1]));
            creees.add(taches.save(t));
        }
        events.publishEvent(new FermeChanged(uid));
        return creees;
    }
}
