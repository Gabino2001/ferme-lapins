package com.ferme.lapins.service;

import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.event.FermeChanged;
import com.ferme.lapins.model.Commande;
import com.ferme.lapins.model.LotEngraissement;
import com.ferme.lapins.model.Mouvement;
import com.ferme.lapins.repository.CommandeRepo;
import com.ferme.lapins.repository.LotEngraissementRepo;
import com.ferme.lapins.repository.MouvementRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import static com.ferme.lapins.util.Util.nz;

@Service
@RequiredArgsConstructor
@Transactional
public class CommandeService {
    private final CommandeRepo commandes;
    private final MouvementRepo mouvements;
    private final LotEngraissementRepo lots;
    private final FermeService ferme;
    private final ApplicationEventPublisher events;

    private Commande own(Long uid, Long id) {
        return commandes.findById(id).filter(c -> uid.equals(c.getUtilisateurId()))
                .orElseThrow(() -> new NoSuchElementException("Commande introuvable"));
    }

    private double total(Commande c) {
        return nz(c.getQuantite()) * (c.getPrixUnitaire() == null ? 0 : c.getPrixUnitaire());
    }

    /** Livre la commande : enregistre la vente (et retire les lapins du lot si un lot est indiqué). */
    public Commande livrer(Long uid, Long id, Long lotId, LocalDate date) {
        Commande c = own(uid, id);
        if (!"EN_ATTENTE".equals(c.getStatut())) throw new IllegalArgumentException("Commande déjà traitée");
        LocalDate d = date == null ? LocalDate.now() : date;
        int q = nz(c.getQuantite());
        double pu = c.getPrixUnitaire() == null ? 0 : c.getPrixUnitaire();
        if (lotId != null) {
            LotEngraissement lot = lots.findById(lotId).filter(l -> uid.equals(l.getUtilisateurId()))
                    .orElseThrow(() -> new NoSuchElementException("Lot introuvable"));
            if (nz(lot.getNombre()) < q) throw new IllegalArgumentException("Le lot ne contient que " + nz(lot.getNombre()) + " lapin(s)");
            ferme.venteLot(uid, lotId, new Dtos.VenteLot(q, pu, d));
        } else {
            Mouvement m = new Mouvement();
            m.setUtilisateurId(uid);
            m.setType("VENTE");
            m.setCategorie(c.getProduit() == null ? "Lapin pour la viande" : c.getProduit());
            m.setQuantite(q);
            m.setMontant(q * pu);
            m.setDate(d);
            mouvements.save(m);
        }
        c.setStatut("LIVREE");
        c.setDateLivraison(d);
        commandes.save(c);
        events.publishEvent(new FermeChanged(uid));
        return c;
    }

    public Commande paiement(Long uid, Long id, double montant) {
        Commande c = own(uid, id);
        if ("ANNULEE".equals(c.getStatut())) throw new IllegalArgumentException("Commande annulée");
        if (montant <= 0) throw new IllegalArgumentException("Montant invalide");
        double paye = (c.getMontantPaye() == null ? 0 : c.getMontantPaye()) + montant;
        if (paye > total(c) + 0.01) throw new IllegalArgumentException("Le paiement dépasse le montant dû");
        c.setMontantPaye(paye);
        commandes.save(c);
        events.publishEvent(new FermeChanged(uid));
        return c;
    }

    public Commande annuler(Long uid, Long id) {
        Commande c = own(uid, id);
        if (!"EN_ATTENTE".equals(c.getStatut())) throw new IllegalArgumentException("Seule une commande en attente peut être annulée");
        c.setStatut("ANNULEE");
        commandes.save(c);
        events.publishEvent(new FermeChanged(uid));
        return c;
    }
}
