package com.ferme.lapins.service;

import com.ferme.lapins.dto.Alerte;
import com.ferme.lapins.model.Client;
import com.ferme.lapins.model.Commande;
import com.ferme.lapins.model.Soin;
import com.ferme.lapins.repository.ClientRepo;
import com.ferme.lapins.repository.CommandeRepo;
import com.ferme.lapins.repository.SoinRepo;
import com.ferme.lapins.dto.CageEtat;
import com.ferme.lapins.model.CasMaladie;
import com.ferme.lapins.model.Guide;
import com.ferme.lapins.model.LotEngraissement;
import com.ferme.lapins.model.Portee;
import com.ferme.lapins.model.Saillie;
import com.ferme.lapins.model.StockItem;
import com.ferme.lapins.repository.CasMaladieRepo;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.repository.LotEngraissementRepo;
import com.ferme.lapins.repository.PorteeRepo;
import com.ferme.lapins.repository.SaillieRepo;
import com.ferme.lapins.repository.StockItemRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import static com.ferme.lapins.util.Util.nz;
import static java.time.temporal.ChronoUnit.DAYS;

/** Moteur d’alertes : transforme les données de la ferme en problèmes + solutions. */
@Service
@RequiredArgsConstructor
public class AlerteService {
    private final LapinRepo lapins;
    private final SaillieRepo saillies;
    private final PorteeRepo portees;
    private final LotEngraissementRepo lots;
    private final CasMaladieRepo cas;
    private final StockItemRepo stock;
    private final SoinRepo soins;
    private final CommandeRepo commandes;
    private final ClientRepo clients;
    private final ParametresService parametres;
    private final CageService cageService;
    private final BilanService bilanService;

    public List<Alerte> calculer(Long uid) {
        List<Alerte> o = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Map<Long, String> noms = new HashMap<>();
        lapins.findByUtilisateurId(uid).forEach(l -> noms.put(l.getId(), l.getNom()));

        for (Saillie s : saillies.findByUtilisateurId(uid)) {
            if (s.getDate() == null) continue;
            long j = DAYS.between(s.getDate(), today);
            long du = DAYS.between(today, s.getDate().plusDays(31));
            String f = noms.getOrDefault(s.getFemelleId(), "?");
            String w = f + " × " + noms.getOrDefault(s.getMaleId(), "?");
            if (s.getPalpation() == null && j >= 12)
                o.add(new Alerte("amb", "Palpation", "Palpation à faire", w + " · saillie il y a " + j + " j",
                        List.of("Palper la lapine vers le 14e jour", "Si vide, la représenter au mâle"), "elevage"));
            if (!"non".equals(s.getPalpation())) {
                if (du < 0)
                    o.add(new Alerte("red", "Urgent", "Mise bas en retard de " + (-du) + " j", w,
                            List.of("Vérifier la lapine et la boîte à nid", "Appeler le vétérinaire si rien sous 2 jours"), "elevage"));
                else if (du <= 3)
                    o.add(new Alerte("amb", "Dans " + du + " j", "Mise bas proche", f + " · prévue le " + s.getDate().plusDays(31),
                            List.of("Installer la boîte à nid avec de la paille", "Limiter le dérangement autour de la cage"), "elevage"));
            }
        }

        for (Portee p : portees.findByUtilisateurId(uid)) {
            if (Boolean.TRUE.equals(p.getSevree()) || p.getDate() == null) continue;
            long du = 35 - DAYS.between(p.getDate(), today);
            String w = "Portée de " + noms.getOrDefault(p.getMereId(), "?");
            if (du <= 0)
                o.add(new Alerte("amb", "Sevrage", "Sevrage à faire", w + " · " + nz(p.getVivants()) + " lapereaux",
                        List.of("Séparer les lapereaux de la mère", "Peser, sexer et répartir par cage", "Aliment de croissance à volonté"), "elevage"));
            else if (du <= 3)
                o.add(new Alerte("blu", "Dans " + du + " j", "Sevrage bientôt", w, List.of("Préparer les cages de sevrage"), "elevage"));
        }

        for (CasMaladie c : cas.findByUtilisateurId(uid)) {
            if (!"EN_TRAITEMENT".equals(c.getStatut())) continue;
            long depuis = c.getDate() == null ? 0 : DAYS.between(c.getDate(), today);
            o.add(new Alerte("red", "Santé", c.getMaladie(), noms.getOrDefault(c.getLapinId(), "?") + " · depuis " + depuis + " j",
                    Guide.solutions(c.getMaladie()), "sante"));
        }

        for (StockItem k : stock.findByUtilisateurId(uid)) {
            double conso = k.getConsoJour() == null ? 0 : k.getConsoJour();
            if (conso <= 0) continue;
            double q = k.getQuantite() == null ? 0 : k.getQuantite();
            double jours = q / conso;
            if (jours < 5)
                o.add(new Alerte(jours < 2 ? "red" : "amb", "Stock", k.getNom() + " : " + (int) jours + " jours restants",
                        q + " " + k.getUnite() + " en réserve", List.of("Commander rapidement"), "argent"));
        }

        for (CageEtat g : cageService.plan(uid)) {
            if (g.surpeuplee())
                o.add(new Alerte("amb", "Cage", "Cage " + g.numero() + " surpeuplée", g.occupation() + " occupants pour " + g.capacite() + " place(s)",
                        List.of("Déplacer des lapins vers une cage libre"), "cages"));
            if (g.melange())
                o.add(new Alerte("amb", "Cage", "Mâle et femelle ensemble", "Cage " + g.numero(),
                        List.of("Séparer sauf si la saillie est voulue", "Évite les gestations non prévues"), "cages"));
            if (g.malade())
                o.add(new Alerte("amb", "Cage", "Cage " + g.numero() + " à désinfecter", "Cas de maladie dans la cage",
                        List.of("Nettoyer et désinfecter après la guérison", "Changer la litière et l’eau"), "cages"));
        }

        for (LotEngraissement l : lots.findByUtilisateurId(uid)) {
            if (l.getDateNaissance() == null) continue;
            long age = DAYS.between(l.getDateNaissance(), today);
            if (age >= 70)
                o.add(new Alerte("blu", "Vente", nz(l.getNombre()) + " lapereaux prêts à vendre", "Cage " + l.getCage() + " · " + (age / 7) + " semaines",
                        List.of("Peser un échantillon du lot", "Comparer le prix du marché à votre coût de revient", "Enregistrer la vente pour mettre le bilan à jour"), "elevage"));
        }

        var bilan = bilanService.calculer(uid, 30);
        if (bilan.resultat() < 0)
            o.add(new Alerte("amb", "Argent", "Vous perdez de l’argent ce mois", "Résultat : " + Math.round(bilan.resultat()),
                    List.of("Comparer le coût de revient au prix de vente", "Réduire le gaspillage d’aliment", "Vendre à l’âge où le lapin est le plus rentable"), "argent"));

        for (Soin s : soins.findByUtilisateurId(uid)) {
            if (s.getDateRealisee() != null || s.getDatePrevue() == null) continue;
            long du = DAYS.between(today, s.getDatePrevue());
            String cible = s.getLapinId() == null ? "Tout le cheptel" : noms.getOrDefault(s.getLapinId(), "?");
            String type = "VACCIN".equals(s.getType()) ? "Vaccin" : "Traitement";
            if (du < 0)
                o.add(new Alerte("red", "En retard", type + " en retard : " + s.getNom(), cible + " · prévu depuis " + (-du) + " j",
                        List.of("Effectuer le soin dès que possible", "Noter le lot du produit utilisé", "Demander conseil au vétérinaire si besoin"), "sante"));
            else if (du <= 7)
                o.add(new Alerte(du <= 2 ? "amb" : "blu", du == 0 ? "Aujourd’hui" : "Dans " + du + " j", type + " à prévoir : " + s.getNom(), cible,
                        List.of("Préparer le produit et le matériel", "Peser les animaux si la dose dépend du poids"), "sante"));
        }

        String devise = parametres.devise(uid);
        Map<Long, String> nomsClients = new HashMap<>();
        for (Client cl : clients.findByUtilisateurId(uid)) nomsClients.put(cl.getId(), cl.getNom());
        for (Commande c : commandes.findByUtilisateurId(uid)) {
            double total = nz(c.getQuantite()) * (c.getPrixUnitaire() == null ? 0 : c.getPrixUnitaire());
            double reste = total - (c.getMontantPaye() == null ? 0 : c.getMontantPaye());
            String qui = nomsClients.getOrDefault(c.getClientId(), "Client");
            String objet = nz(c.getQuantite()) + " × " + c.getProduit();
            if ("EN_ATTENTE".equals(c.getStatut()) && c.getDateLivraisonPrevue() != null) {
                long du = DAYS.between(today, c.getDateLivraisonPrevue());
                if (du < 0)
                    o.add(new Alerte("red", "En retard", "Livraison en retard : " + qui, objet + " · prévue depuis " + (-du) + " j",
                            List.of("Contacter le client pour fixer une nouvelle date", "Vérifier qu’un lot est prêt à la vente"), "clients"));
                else if (du <= 3)
                    o.add(new Alerte("amb", du == 0 ? "Aujourd’hui" : "Dans " + du + " j", "Livraison à préparer : " + qui, objet,
                            List.of("Préparer et peser les lapins", "Prévenir le client"), "clients"));
            }
            if ("LIVREE".equals(c.getStatut()) && reste > 0 && c.getDateLivraison() != null && DAYS.between(c.getDateLivraison(), today) >= 7)
                o.add(new Alerte("amb", "Impayé", "Paiement en attente : " + qui, "Reste " + Math.round(reste) + " " + devise + " à encaisser",
                        List.of("Relancer le client", "Enregistrer le paiement dès réception"), "clients"));
        }

        List<String> ordre = List.of("red", "amb", "blu");
        o.sort(Comparator.comparingInt(a -> ordre.indexOf(a.severite())));
        return o;
    }
}
