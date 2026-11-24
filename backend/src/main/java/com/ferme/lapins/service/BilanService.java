package com.ferme.lapins.service;

import com.ferme.lapins.dto.Bilan;
import com.ferme.lapins.model.Mouvement;
import com.ferme.lapins.repository.MouvementRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import static com.ferme.lapins.util.Util.nz;

@Service
@RequiredArgsConstructor
public class BilanService {
    private final MouvementRepo mouvements;

    public Bilan calculer(Long uid, int jours) {
        LocalDate limite = LocalDate.now().minusDays(jours);
        double v = 0, d = 0;
        int n = 0;
        Map<String, Double> cat = new LinkedHashMap<>();
        for (Mouvement m : mouvements.findByUtilisateurId(uid)) {
            if (m.getDate() == null || m.getDate().isBefore(limite)) continue;
            double montant = m.getMontant() == null ? 0 : m.getMontant();
            if ("VENTE".equals(m.getType())) { v += montant; n += nz(m.getQuantite()); }
            else { d += montant; cat.merge(m.getCategorie() == null ? "Autre" : m.getCategorie(), montant, Double::sum); }
        }
        return new Bilan(v, d, v - d, n, n > 0 ? d / n : 0, n > 0 ? v / n : 0, cat);
    }
}
