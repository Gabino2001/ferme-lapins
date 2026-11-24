package com.ferme.lapins.service;

import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.event.FermeChanged;
import com.ferme.lapins.model.CasMaladie;
import com.ferme.lapins.model.Lapin;
import com.ferme.lapins.model.LotEngraissement;
import com.ferme.lapins.model.Mouvement;
import com.ferme.lapins.model.Pesee;
import com.ferme.lapins.model.Portee;
import com.ferme.lapins.model.Proprio;
import com.ferme.lapins.model.Saillie;
import com.ferme.lapins.model.StockItem;
import com.ferme.lapins.repository.CasMaladieRepo;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.repository.LotEngraissementRepo;
import com.ferme.lapins.repository.MouvementRepo;
import com.ferme.lapins.repository.OwnedRepo;
import com.ferme.lapins.repository.PeseeRepo;
import com.ferme.lapins.repository.PorteeRepo;
import com.ferme.lapins.repository.SaillieRepo;
import com.ferme.lapins.repository.StockItemRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/** Actions métier de la ferme : palpation, mise bas, sevrage, ventes, pesées... */
@Service
@RequiredArgsConstructor
@Transactional
public class FermeService {
    private final LapinRepo lapins;
    private final PeseeRepo pesees;
    private final SaillieRepo saillies;
    private final PorteeRepo portees;
    private final LotEngraissementRepo lots;
    private final CasMaladieRepo cas;
    private final StockItemRepo stock;
    private final MouvementRepo mouvements;
    private final ApplicationEventPublisher events;

    private <T extends Proprio> T own(OwnedRepo<T> r, Long id, Long uid) {
        return r.findById(id).filter(x -> uid.equals(x.getUtilisateurId()))
                .orElseThrow(() -> new NoSuchElementException("Introuvable"));
    }
    private void done(Long uid) { events.publishEvent(new FermeChanged(uid)); }

    public Saillie palpation(Long uid, Long id, String resultat) {
        if (!List.of("ok", "non").contains(resultat)) throw new IllegalArgumentException("resultat doit valoir ok ou non");
        Saillie s = own(saillies, id, uid);
        s.setPalpation(resultat);
        done(uid);
        return saillies.save(s);
    }

    public Portee miseBas(Long uid, Long saillieId, Dtos.MiseBas d) {
        Saillie s = own(saillies, saillieId, uid);
        Portee p = new Portee();
        p.setUtilisateurId(uid);
        p.setMereId(s.getFemelleId());
        p.setDate(d.date() == null ? LocalDate.now() : d.date());
        p.setNes(d.nes());
        p.setVivants(d.vivants());
        p.setSevres(0);
        p.setSevree(false);
        saillies.delete(s);
        done(uid);
        return portees.save(p);
    }

    public LotEngraissement sevrage(Long uid, Long porteeId, Dtos.Sevrage d) {
        Portee p = own(portees, porteeId, uid);
        if (Boolean.TRUE.equals(p.getSevree())) throw new IllegalArgumentException("Portée déjà sevrée");
        p.setSevres(d.nombre());
        p.setSevree(true);
        portees.save(p);
        LotEngraissement lot = null;
        if (d.nombre() > 0) {
            lot = new LotEngraissement();
            lot.setUtilisateurId(uid);
            lot.setMereId(p.getMereId());
            lot.setNombre(d.nombre());
            lot.setCage(d.cage());
            lot.setDateNaissance(p.getDate());
            lot = lots.save(lot);
        }
        done(uid);
        return lot;
    }

    public Mouvement venteLot(Long uid, Long lotId, Dtos.VenteLot d) {
        LotEngraissement l = own(lots, lotId, uid);
        int q = Math.min(d.quantite(), l.getNombre());
        if (q <= 0) throw new IllegalArgumentException("Quantité invalide");
        Mouvement m = new Mouvement();
        m.setUtilisateurId(uid);
        m.setType("VENTE");
        m.setCategorie("Lapin pour la viande");
        m.setQuantite(q);
        m.setMontant(q * d.prixUnitaire());
        m.setDate(d.date() == null ? LocalDate.now() : d.date());
        mouvements.save(m);
        l.setNombre(l.getNombre() - q);
        if (l.getNombre() <= 0) lots.delete(l); else lots.save(l);
        done(uid);
        return m;
    }

    public void perteLot(Long uid, Long lotId, int nombre) {
        LotEngraissement l = own(lots, lotId, uid);
        l.setNombre(l.getNombre() - Math.min(Math.max(nombre, 0), l.getNombre()));
        if (l.getNombre() <= 0) lots.delete(l); else lots.save(l);
        done(uid);
    }

    public List<Pesee> pesees(Long uid, Long lapinId) {
        own(lapins, lapinId, uid);
        return pesees.findByLapinIdAndUtilisateurIdOrderByDate(lapinId, uid);
    }

    public Pesee pesee(Long uid, Long lapinId, Dtos.PeseeReq d) {
        Lapin l = own(lapins, lapinId, uid);
        Pesee p = new Pesee();
        p.setUtilisateurId(uid);
        p.setLapinId(lapinId);
        p.setPoids(d.poids());
        p.setDate(d.date() == null ? LocalDate.now() : d.date());
        pesees.save(p);
        List<Pesee> toutes = pesees.findByLapinIdAndUtilisateurIdOrderByDate(lapinId, uid);
        l.setPoids(toutes.get(toutes.size() - 1).getPoids());
        lapins.save(l);
        done(uid);
        return p;
    }

    public CasMaladie statutCas(Long uid, Long id, String statut) {
        if (!List.of("EN_TRAITEMENT", "GUERI", "DECEDE").contains(statut)) throw new IllegalArgumentException("Statut invalide");
        CasMaladie c = own(cas, id, uid);
        c.setStatut(statut);
        done(uid);
        return cas.save(c);
    }

    public StockItem reappro(Long uid, Long id, double quantite) {
        StockItem k = own(stock, id, uid);
        k.setQuantite((k.getQuantite() == null ? 0 : k.getQuantite()) + quantite);
        done(uid);
        return stock.save(k);
    }
}
