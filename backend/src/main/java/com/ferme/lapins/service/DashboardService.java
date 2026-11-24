package com.ferme.lapins.service;

import com.ferme.lapins.dto.Alerte;
import com.ferme.lapins.dto.Dashboard;
import com.ferme.lapins.repository.CasMaladieRepo;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.security.Auth;
import com.ferme.lapins.repository.LotEngraissementRepo;
import com.ferme.lapins.repository.PorteeRepo;
import com.ferme.lapins.repository.SaillieRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static com.ferme.lapins.util.Util.nz;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final AlerteService alertes;
    private final CageService cages;
    private final LapinRepo lapins;
    private final SaillieRepo saillies;
    private final PorteeRepo portees;
    private final LotEngraissementRepo lots;
    private final CasMaladieRepo cas;

    public Dashboard dashboard(Long uid) {
        List<Alerte> al = alertes.calculer(uid);
        if (!Auth.estProprietaire()) al = al.stream().filter(a -> !List.of("argent", "clients").contains(a.section())).toList();
        int score = 100;
        for (Alerte a : al) score -= switch (a.severite()) { case "red" -> 22; case "amb" -> 9; default -> 3; };
        score = Math.max(5, score);
        boolean rouge = al.stream().anyMatch(a -> "red".equals(a.severite()));
        String statut = score >= 85 ? "Ferme sous contrôle" : rouge ? "Action requise" : "À surveiller";
        String resume = al.isEmpty() ? "Rien à signaler" : al.size() + " point" + (al.size() > 1 ? "s" : "") + " à traiter";

        Map<String, Integer> k = new LinkedHashMap<>();
        k.put("lapins", lapins.findByUtilisateurId(uid).size());
        k.put("gestantes", (int) saillies.findByUtilisateurId(uid).stream().filter(s -> !"non".equals(s.getPalpation())).count());
        k.put("sousLaMere", portees.findByUtilisateurId(uid).stream().filter(p -> !Boolean.TRUE.equals(p.getSevree())).mapToInt(p -> nz(p.getVivants())).sum());
        k.put("engraissement", lots.findByUtilisateurId(uid).stream().mapToInt(l -> nz(l.getNombre())).sum());
        k.put("malades", (int) cas.findByUtilisateurId(uid).stream().filter(c -> "EN_TRAITEMENT".equals(c.getStatut())).count());
        k.put("cagesLibres", (int) cages.plan(uid).stream().filter(c -> c.occupation() == 0).count());
        return new Dashboard(score, statut, resume, k, al);
    }
}
