package com.ferme.lapins.service;

import com.ferme.lapins.dto.CageEtat;
import com.ferme.lapins.model.Cage;
import com.ferme.lapins.model.CasMaladie;
import com.ferme.lapins.model.Lapin;
import com.ferme.lapins.repository.CageRepo;
import com.ferme.lapins.repository.CasMaladieRepo;
import com.ferme.lapins.repository.LapinRepo;
import com.ferme.lapins.repository.LotEngraissementRepo;
import com.ferme.lapins.repository.PorteeRepo;
import com.ferme.lapins.repository.SaillieRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import static com.ferme.lapins.util.Util.nz;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
public class CageService {
    private final CageRepo cages;
    private final LapinRepo lapins;
    private final SaillieRepo saillies;
    private final PorteeRepo portees;
    private final LotEngraissementRepo lots;
    private final CasMaladieRepo cas;

    public List<CageEtat> plan(Long uid) {
        var ls = lapins.findByUtilisateurId(uid);
        var ss = saillies.findByUtilisateurId(uid);
        var ps = portees.findByUtilisateurId(uid);
        var lo = lots.findByUtilisateurId(uid);
        Set<Long> malades = cas.findByUtilisateurId(uid).stream().filter(c -> "EN_TRAITEMENT".equals(c.getStatut()))
                .map(CasMaladie::getLapinId).collect(toSet());
        LocalDate today = LocalDate.now();
        List<CageEtat> out = new ArrayList<>();
        for (Cage g : cages.findByUtilisateurId(uid)) {
            var rs = ls.stream().filter(r -> Objects.equals(g.getNumero(), r.getCage())).toList();
            Set<Long> ids = rs.stream().map(Lapin::getId).collect(toSet());
            int occ = rs.size();
            occ += ps.stream().filter(p -> !Boolean.TRUE.equals(p.getSevree()) && ids.contains(p.getMereId())).mapToInt(p -> nz(p.getVivants())).sum();
            occ += lo.stream().filter(l -> Objects.equals(g.getNumero(), l.getCage())).mapToInt(l -> nz(l.getNombre())).sum();
            boolean malade = ids.stream().anyMatch(malades::contains);
            boolean melange = rs.stream().anyMatch(r -> "M".equals(r.getSexe())) && rs.stream().anyMatch(r -> "F".equals(r.getSexe()));
            boolean bientot = ss.stream().anyMatch(s -> !"non".equals(s.getPalpation()) && ids.contains(s.getFemelleId())
                    && DAYS.between(today, s.getDate().plusDays(31)) <= 3);
            int cap = nz(g.getCapacite());
            String etat, lib;
            if (occ > cap) { etat = "red"; lib = "Surpeuplée"; }
            else if (malade) { etat = "red"; lib = "Cas de maladie"; }
            else if (melange) { etat = "amb"; lib = "Mâle et femelle"; }
            else if (bientot) { etat = "amb"; lib = "Mise bas proche"; }
            else if (occ > 0) { etat = "ok"; lib = "Occupée"; }
            else { etat = "free"; lib = "Libre"; }
            out.add(new CageEtat(g.getId(), g.getNumero(), g.getType(), cap, occ, etat, lib,
                    rs.stream().map(Lapin::getNom).toList(), occ > cap, melange, malade));
        }
        out.sort(Comparator.comparing(CageEtat::numero, Comparator.nullsLast(String::compareTo)));
        return out;
    }
}
