package com.ferme.lapins.service;

import com.ferme.lapins.dto.LignePlanning;
import com.ferme.lapins.dto.Parente;
import com.ferme.lapins.model.Lapin;
import com.ferme.lapins.model.Portee;
import com.ferme.lapins.model.Saillie;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.repository.PorteeRepo;
import com.ferme.lapins.repository.SaillieRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static com.ferme.lapins.util.Util.nz;
import static java.time.temporal.ChronoUnit.DAYS;

/** Planning de reproduction : où en est chaque lapine, quand la saillir, avec quel mâle. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanningService {
    private static final List<String> ORDRE = List.of("PRETE", "GESTANTE", "ALLAITANTE", "REPOS", "TROP_JEUNE");
    private final LapinRepo lapins;
    private final SaillieRepo saillies;
    private final PorteeRepo portees;
    private final GenealogieService genealogie;

    /** delai : nombre de jours après la mise bas avant de resaillir la lapine (repère modifiable). */
    public List<LignePlanning> calculer(Long uid, int delai) {
        LocalDate today = LocalDate.now();
        List<Lapin> ls = lapins.findByUtilisateurId(uid);
        List<Lapin> males = ls.stream().filter(l -> "M".equals(l.getSexe())).toList();
        List<Saillie> ss = saillies.findByUtilisateurId(uid);
        List<Portee> ps = portees.findByUtilisateurId(uid);
        List<LignePlanning> out = new ArrayList<>();

        for (Lapin f : ls) {
            if (!"F".equals(f.getSexe())) continue;
            Saillie gest = ss.stream()
                    .filter(s -> Objects.equals(s.getFemelleId(), f.getId()) && !"non".equals(s.getPalpation()) && s.getDate() != null)
                    .findFirst().orElse(null);
            List<Portee> mes = ps.stream()
                    .filter(p -> Objects.equals(p.getMereId(), f.getId()) && p.getDate() != null)
                    .sorted(Comparator.comparing(Portee::getDate)).toList();
            Portee derniere = mes.isEmpty() ? null : mes.get(mes.size() - 1);

            String conseil = null;
            if (mes.size() >= 3) {
                double moy = mes.stream().mapToInt(p -> nz(p.getNes())).average().orElse(0);
                if (moy < 5) conseil = "Portées petites (" + Math.round(moy * 10) / 10.0 + " nés en moyenne) : envisager la réforme de cette lapine.";
            }

            String etat, detail;
            LocalDate dateCle;
            if (gest != null) {
                etat = "GESTANTE";
                dateCle = gest.getDate().plusDays(31);
                detail = (gest.getPalpation() == null ? "Gestation à confirmer par palpation" : "Gestation confirmée")
                        + " · mise bas prévue le " + dateCle;
            } else if (derniere != null && !Boolean.TRUE.equals(derniere.getSevree())) {
                etat = "ALLAITANTE";
                dateCle = derniere.getDate().plusDays(35);
                detail = "Allaite " + nz(derniere.getVivants()) + " lapereaux · sevrage prévu le " + dateCle
                        + " · ressaillie conseillée à partir du " + derniere.getDate().plusDays(delai);
            } else if (derniere != null) {
                LocalDate prete = derniere.getDate().plusDays(Math.max(delai, 0));
                dateCle = prete;
                if (!prete.isAfter(today)) {
                    etat = "PRETE";
                    detail = "Prête à être saillie (dernière mise bas le " + derniere.getDate() + ")";
                } else {
                    etat = "REPOS";
                    detail = "Au repos · prête à partir du " + prete;
                }
            } else {
                long age = f.getDateNaissance() == null ? 999 : DAYS.between(f.getDateNaissance(), today);
                if (age < 150) {
                    etat = "TROP_JEUNE";
                    dateCle = f.getDateNaissance().plusDays(150);
                    detail = "Trop jeune pour la reproduction · prête vers le " + dateCle;
                } else {
                    etat = "PRETE";
                    dateCle = today;
                    detail = "Prête pour une première saillie";
                }
            }

            Long maleId = null;
            String maleNom = null, niveau = null;
            if ("PRETE".equals(etat)) {
                Lapin meilleur = null;
                Parente meilleurP = null;
                for (Lapin m : males) {
                    Parente p = genealogie.analyser(ls, f.getId(), m.getId());
                    if (meilleurP == null || GenealogieService.rang(p.niveau()) < GenealogieService.rang(meilleurP.niveau())) {
                        meilleur = m;
                        meilleurP = p;
                    }
                }
                if (meilleur != null) {
                    maleId = meilleur.getId();
                    maleNom = meilleur.getNom();
                    niveau = meilleurP.niveau();
                }
            }
            out.add(new LignePlanning(f.getId(), f.getNom(), f.getCage(), etat, detail, dateCle, maleId, maleNom, niveau, conseil));
        }
        out.sort(Comparator.comparingInt((LignePlanning l) -> ORDRE.indexOf(l.etat()))
                .thenComparing(LignePlanning::dateCle, Comparator.nullsLast(Comparator.naturalOrder())));
        return out;
    }
}
