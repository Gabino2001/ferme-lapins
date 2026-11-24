package com.ferme.lapins.service;

import com.ferme.lapins.dto.Statistiques;
import com.ferme.lapins.model.Lapin;
import com.ferme.lapins.model.Mouvement;
import com.ferme.lapins.model.Portee;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.repository.MouvementRepo;
import com.ferme.lapins.repository.PorteeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import static com.ferme.lapins.util.Util.nz;

@Service
@RequiredArgsConstructor
public class StatistiquesService {
    private final PorteeRepo portees;
    private final LapinRepo lapins;
    private final MouvementRepo mouvements;

    private static double r1(double x) { return Math.round(x * 10) / 10.0; }

    public Statistiques calculer(Long uid) {
        List<Portee> ps = portees.findByUtilisateurId(uid);
        Map<Long, String> noms = new HashMap<>();
        for (Lapin l : lapins.findByUtilisateurId(uid)) noms.put(l.getId(), l.getNom());

        int n = ps.size();
        double nes = ps.stream().mapToInt(p -> nz(p.getNes())).sum();
        double vivants = ps.stream().mapToInt(p -> nz(p.getVivants())).sum();
        List<Portee> sevrees = ps.stream().filter(p -> Boolean.TRUE.equals(p.getSevree())).toList();
        double nesSevrees = sevrees.stream().mapToInt(p -> nz(p.getNes())).sum();
        double sevres = sevrees.stream().mapToInt(p -> nz(p.getSevres())).sum();
        double mortalite = nesSevrees > 0 ? (nesSevrees - sevres) / nesSevrees * 100 : 0;

        Map<Long, int[]> parMere = new HashMap<>();
        for (Portee p : ps) {
            int[] a = parMere.computeIfAbsent(p.getMereId(), k -> new int[3]);
            a[0]++;
            a[1] += nz(p.getNes());
            if (Boolean.TRUE.equals(p.getSevree())) a[2] += nz(p.getSevres());
        }
        List<Statistiques.LapineStat> classement = new ArrayList<>();
        parMere.forEach((id, a) -> classement.add(new Statistiques.LapineStat(
                id, noms.getOrDefault(id, "?"), a[0], a[1], a[2], r1((double) a[1] / a[0]))));
        classement.sort(Comparator.comparingInt(Statistiques.LapineStat::sevres).reversed());

        List<Mouvement> tous = mouvements.findByUtilisateurId(uid);
        LocalDate limite = LocalDate.now().minusDays(90);
        double aliment = 0;
        int vendus = 0;
        for (Mouvement m : tous) {
            if (m.getDate() == null || m.getDate().isBefore(limite)) continue;
            if ("VENTE".equals(m.getType())) vendus += nz(m.getQuantite());
            else if ("Aliment".equals(m.getCategorie())) aliment += m.getMontant() == null ? 0 : m.getMontant();
        }

        List<Statistiques.MoisStat> mensuel = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            double v = 0, d = 0;
            for (Mouvement m : tous) {
                if (m.getDate() == null || !YearMonth.from(m.getDate()).equals(ym)) continue;
                double x = m.getMontant() == null ? 0 : m.getMontant();
                if ("VENTE".equals(m.getType())) v += x; else d += x;
            }
            mensuel.add(new Statistiques.MoisStat(ym.toString(), v, d, v - d));
        }
        return new Statistiques(n, r1(n > 0 ? nes / n : 0), r1(n > 0 ? vivants / n : 0), r1(mortalite), vendus,
                vendus > 0 ? aliment / vendus : 0, classement.stream().limit(10).toList(), mensuel);
    }
}
