package com.ferme.lapins.service;

import com.ferme.lapins.dto.Alerte;
import com.ferme.lapins.dto.Bilan;
import com.ferme.lapins.dto.Dashboard;
import com.ferme.lapins.dto.LignePlanning;
import com.ferme.lapins.dto.Previsions;
import com.ferme.lapins.dto.Statistiques;
import com.ferme.lapins.model.CasMaladie;
import com.ferme.lapins.model.Lapin;
import com.ferme.lapins.model.StockItem;
import com.ferme.lapins.repository.CasMaladieRepo;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.repository.StockItemRepo;
import com.ferme.lapins.security.Auth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Résumé texte de la ferme, donné à l'assistant pour qu'il réponde avec les vraies données. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContexteFermeService {
    private final DashboardService dashboard;
    private final StatistiquesService statistiques;
    private final BilanService bilans;
    private final PlanningService planning;
    private final PrevisionService previsions;
    private final CasMaladieRepo cas;
    private final LapinRepo lapins;
    private final StockItemRepo stock;
    private final ParametresService parametres;

    public String construire(Long uid) {
        boolean proprietaire = Auth.estProprietaire();
        StringBuilder sb = new StringBuilder();
        sb.append("Date du jour : ").append(LocalDate.now()).append('\n');
        sb.append("Devise des montants : ").append(parametres.devise(uid)).append('\n');

        Dashboard d = dashboard.dashboard(uid);
        sb.append("Effectifs : ").append(d.kpis()).append(" ; score de la ferme : ").append(d.score()).append("/100\n");
        sb.append("Alertes en cours :\n");
        List<Alerte> al = d.alertes();
        if (al.isEmpty()) sb.append("- aucune\n");
        for (Alerte a : al.stream().limit(12).toList()) sb.append("- ").append(a.titre()).append(" : ").append(a.detail()).append('\n');

        Statistiques s = statistiques.calculer(uid);
        sb.append("Reproduction : ").append(s.nbPortees()).append(" portées, ").append(s.tailleMoyennePortee()).append(" nés et ")
                .append(s.vivantsMoyens()).append(" vivants en moyenne, mortalité naissance-sevrage ").append(s.mortaliteSevrage()).append(" %\n");
        for (Statistiques.LapineStat l : s.classement().stream().limit(3).toList()) {
            sb.append("- ").append(l.nom()).append(" : ").append(l.portees()).append(" portées, ").append(l.sevres()).append(" sevrés\n");
        }

        Map<Long, String> noms = new HashMap<>();
        for (Lapin l : lapins.findByUtilisateurId(uid)) noms.put(l.getId(), l.getNom());
        sb.append("Maladies en cours :\n");
        boolean aucune = true;
        for (CasMaladie c : cas.findByUtilisateurId(uid)) {
            if (!"EN_TRAITEMENT".equals(c.getStatut())) continue;
            aucune = false;
            sb.append("- ").append(c.getMaladie()).append(" chez ").append(noms.getOrDefault(c.getLapinId(), "?")).append(" depuis le ").append(c.getDate()).append('\n');
        }
        if (aucune) sb.append("- aucune\n");

        sb.append("Stock :\n");
        for (StockItem k : stock.findByUtilisateurId(uid)) {
            double conso = k.getConsoJour() == null ? 0 : k.getConsoJour();
            sb.append("- ").append(k.getNom()).append(" : ").append(k.getQuantite()).append(' ').append(k.getUnite());
            if (conso > 0) sb.append(" (environ ").append((int) ((k.getQuantite() == null ? 0 : k.getQuantite()) / conso)).append(" jours)");
            sb.append('\n');
        }

        sb.append("Planning des lapines : ");
        Map<String, Integer> etats = new HashMap<>();
        for (LignePlanning l : planning.calculer(uid, 21)) etats.merge(l.etat(), 1, Integer::sum);
        sb.append(etats).append('\n');

        Previsions p = previsions.calculer(uid);
        sb.append("À venir dans 30 jours :\n");
        for (Previsions.Evenement e : p.evenements().stream().limit(10).toList()) {
            sb.append("- ").append(e.date()).append(" : ").append(e.titre()).append('\n');
        }

        if (proprietaire) {
            Bilan b = bilans.calculer(uid, 30);
            sb.append("Argent sur 30 jours : ventes ").append(Math.round(b.ventes())).append(", dépenses ").append(Math.round(b.depenses()))
                    .append(", résultat ").append(Math.round(b.resultat())).append(", coût de revient par lapin vendu ").append(Math.round(b.coutRevient()))
                    .append(", prix de vente moyen ").append(Math.round(b.prixMoyen())).append(", dépenses par poste ").append(b.depensesParPoste()).append('\n');
        }
        return sb.toString();
    }
}
