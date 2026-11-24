package com.ferme.lapins.service;

import com.ferme.lapins.dto.Previsions;
import com.ferme.lapins.model.*;
import com.ferme.lapins.repository.*;
import com.ferme.lapins.security.Auth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static com.ferme.lapins.util.Util.nz;

/** Ce qui va se passer dans les 30 prochains jours (mises bas, sevrages, ventes, soins, stock). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrevisionService {
    private final SaillieRepo saillies;
    private final PorteeRepo portees;
    private final LotEngraissementRepo lots;
    private final SoinRepo soins;
    private final StockItemRepo stock;
    private final LapinRepo lapins;
    private final MouvementRepo mouvements;
    private final CommandeRepo commandes;
    private final ClientRepo clients;

    private static LocalDate dep(LocalDate d, LocalDate today) { return d.isBefore(today) ? today : d; }

    public Previsions calculer(Long uid) {
        LocalDate today = LocalDate.now();
        LocalDate limite = today.plusDays(30);
        boolean proprietaire = Auth.estProprietaire();
        Map<Long, String> noms = new HashMap<>();
        lapins.findByUtilisateurId(uid).forEach(l -> noms.put(l.getId(), l.getNom()));

        List<Portee> ps = portees.findByUtilisateurId(uid);
        double moyVivants = ps.stream().filter(p -> p.getVivants() != null).mapToInt(Portee::getVivants).average().orElse(7);

        List<Previsions.Evenement> ev = new ArrayList<>();
        int lapereaux = 0;
        for (Saillie s : saillies.findByUtilisateurId(uid)) {
            if (s.getDate() == null || "non".equals(s.getPalpation())) continue;
            LocalDate mb = s.getDate().plusDays(31);
            if (mb.isAfter(limite)) continue;
            ev.add(new Previsions.Evenement("MISE_BAS", "Mise bas de " + noms.getOrDefault(s.getFemelleId(), "?"), dep(mb, today),
                    (s.getPalpation() == null ? "Gestation à confirmer" : "Gestation confirmée") + (mb.isBefore(today) ? " · en retard" : "")));
            lapereaux += (int) Math.round(moyVivants);
        }
        for (Portee p : ps) {
            if (Boolean.TRUE.equals(p.getSevree()) || p.getDate() == null) continue;
            LocalDate sv = p.getDate().plusDays(35);
            if (sv.isAfter(limite)) continue;
            ev.add(new Previsions.Evenement("SEVRAGE", "Sevrage de la portée de " + noms.getOrDefault(p.getMereId(), "?"), dep(sv, today),
                    nz(p.getVivants()) + " lapereaux"));
        }
        int prets = 0;
        for (LotEngraissement l : lots.findByUtilisateurId(uid)) {
            if (l.getDateNaissance() == null) continue;
            LocalDate pret = l.getDateNaissance().plusDays(70);
            if (pret.isAfter(limite)) continue;
            ev.add(new Previsions.Evenement("VENTE", nz(l.getNombre()) + " lapins prêts à vendre", dep(pret, today), "Cage " + l.getCage()));
            prets += nz(l.getNombre());
        }
        for (Soin s : soins.findByUtilisateurId(uid)) {
            if (s.getDateRealisee() != null || s.getDatePrevue() == null || s.getDatePrevue().isAfter(limite)) continue;
            ev.add(new Previsions.Evenement("SOIN", ("VACCIN".equals(s.getType()) ? "Vaccin : " : "Traitement : ") + s.getNom(), dep(s.getDatePrevue(), today),
                    s.getLapinId() == null ? "Tout le cheptel" : noms.getOrDefault(s.getLapinId(), "?")));
        }

        Double commandesAttente = null;
        Double revenu = null;
        if (proprietaire) {
            Map<Long, String> nomsClients = new HashMap<>();
            clients.findByUtilisateurId(uid).forEach(c -> nomsClients.put(c.getId(), c.getNom()));
            double attente = 0;
            for (Commande c : commandes.findByUtilisateurId(uid)) {
                if (!"EN_ATTENTE".equals(c.getStatut())) continue;
                attente += nz(c.getQuantite()) * (c.getPrixUnitaire() == null ? 0 : c.getPrixUnitaire());
                if (c.getDateLivraisonPrevue() != null && !c.getDateLivraisonPrevue().isAfter(limite)) {
                    ev.add(new Previsions.Evenement("LIVRAISON", "Livraison : " + nomsClients.getOrDefault(c.getClientId(), "Client"),
                            dep(c.getDateLivraisonPrevue(), today), nz(c.getQuantite()) + " × " + c.getProduit()));
                }
            }
            commandesAttente = attente;
            LocalDate depuis = today.minusDays(90);
            double ventes = 0;
            int vendus = 0;
            for (Mouvement m : mouvements.findByUtilisateurId(uid)) {
                if (!"VENTE".equals(m.getType()) || m.getDate() == null || m.getDate().isBefore(depuis)) continue;
                ventes += m.getMontant() == null ? 0 : m.getMontant();
                vendus += nz(m.getQuantite());
            }
            if (vendus > 0 && prets > 0) revenu = prets * (ventes / vendus);
        }
        ev.sort(Comparator.comparing(Previsions.Evenement::date));

        List<Previsions.Rupture> ruptures = new ArrayList<>();
        for (StockItem k : stock.findByUtilisateurId(uid)) {
            double conso = k.getConsoJour() == null ? 0 : k.getConsoJour();
            if (conso <= 0) continue;
            int jours = (int) Math.floor((k.getQuantite() == null ? 0 : k.getQuantite()) / conso);
            if (jours <= 60) ruptures.add(new Previsions.Rupture(k.getNom(), today.plusDays(jours), jours));
        }
        ruptures.sort(Comparator.comparingInt(Previsions.Rupture::jours));
        return new Previsions(ev, lapereaux, prets, ruptures, revenu, commandesAttente);
    }
}
