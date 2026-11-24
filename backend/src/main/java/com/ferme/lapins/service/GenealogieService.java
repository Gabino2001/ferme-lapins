package com.ferme.lapins.service;

import com.ferme.lapins.dto.Genealogie;
import com.ferme.lapins.dto.Parente;
import com.ferme.lapins.model.Lapin;
import com.ferme.lapins.repository.LapinRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Arbre généalogique et détection de consanguinité (3 générations, d'après les parents enregistrés). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenealogieService {
    private final LapinRepo lapins;

    public static int rang(String niveau) {
        return switch (niveau) { case "ELEVE" -> 3; case "MOYEN" -> 2; case "FAIBLE" -> 1; default -> 0; };
    }

    public Parente analyser(Long uid, Long femelleId, Long maleId) {
        return analyser(lapins.findByUtilisateurId(uid), femelleId, maleId);
    }

    public Parente analyser(List<Lapin> ls, Long femelleId, Long maleId) {
        Map<Long, Lapin> idx = index(ls);
        Map<Long, Integer> af = ancetres(idx, femelleId);
        Map<Long, Integer> am = ancetres(idx, maleId);

        Integer d = af.get(maleId);
        if (d != null) {
            return new Parente(d <= 2 ? "ELEVE" : "MOYEN",
                    "Le mâle est " + (d == 1 ? "le père" : d == 2 ? "un grand-parent" : "un arrière-grand-parent") + " de la femelle.");
        }
        d = am.get(femelleId);
        if (d != null) {
            return new Parente(d <= 2 ? "ELEVE" : "MOYEN",
                    "La femelle est " + (d == 1 ? "la mère" : d == 2 ? "un grand-parent" : "un arrière-grand-parent") + " du mâle.");
        }

        int meilleur = Integer.MAX_VALUE;
        Long ancetre = null;
        for (Map.Entry<Long, Integer> e : af.entrySet()) {
            Integer gm = am.get(e.getKey());
            if (gm == null) continue;
            int somme = e.getValue() + gm;
            if (somme < meilleur) { meilleur = somme; ancetre = e.getKey(); }
        }
        if (ancetre == null) {
            return new Parente("AUCUN", "Aucun lien de parenté connu sur 3 générations (d’après les parents enregistrés).");
        }
        String commun = nom(idx, ancetre);
        if (meilleur == 2) {
            long communs = af.keySet().stream().filter(k -> am.containsKey(k) && af.get(k) + am.get(k) == 2).count();
            return communs >= 2
                    ? new Parente("ELEVE", "Frère et sœur (mêmes parents).")
                    : new Parente("ELEVE", "Demi-frère et demi-sœur (parent commun : " + commun + ").");
        }
        if (meilleur == 3) return new Parente("ELEVE", "Oncle et nièce, ou tante et neveu (ancêtre commun : " + commun + ").");
        if (meilleur == 4) return new Parente("MOYEN", "Cousins germains (ancêtre commun : " + commun + ").");
        return new Parente("FAIBLE", "Cousins éloignés (ancêtre commun : " + commun + ").");
    }

    public Genealogie arbre(Long uid, Long id) {
        List<Lapin> ls = lapins.findByUtilisateurId(uid);
        Map<Long, Lapin> idx = index(ls);
        Lapin l = idx.get(id);
        if (l == null) throw new NoSuchElementException("Lapin introuvable");
        List<Genealogie.Enfant> enfants = ls.stream()
                .filter(x -> id.equals(x.getPereId()) || id.equals(x.getMereId()))
                .map(x -> new Genealogie.Enfant(x.getId(), x.getNom(), x.getSexe()))
                .toList();
        return new Genealogie(noeud(idx, l, 3), enfants);
    }

    private Genealogie.Noeud noeud(Map<Long, Lapin> idx, Lapin l, int profondeur) {
        Genealogie.Noeud pere = null, mere = null;
        if (profondeur > 0) {
            pere = parent(idx, l.getPereId(), l.getPere(), "M", profondeur - 1);
            mere = parent(idx, l.getMereId(), l.getMere(), "F", profondeur - 1);
        }
        return new Genealogie.Noeud(l.getId(), l.getNom(), l.getSexe(), pere, mere);
    }

    private Genealogie.Noeud parent(Map<Long, Lapin> idx, Long pid, String libre, String sexe, int profondeur) {
        if (pid != null && idx.containsKey(pid)) return noeud(idx, idx.get(pid), profondeur);
        if (libre != null && !libre.isBlank()) return new Genealogie.Noeud(null, libre.trim(), sexe, null, null);
        return null;
    }

    private Map<Long, Lapin> index(List<Lapin> ls) {
        Map<Long, Lapin> idx = new HashMap<>();
        for (Lapin l : ls) idx.put(l.getId(), l);
        return idx;
    }

    private String nom(Map<Long, Lapin> idx, Long id) {
        Lapin l = idx.get(id);
        return l == null ? "inconnu" : l.getNom();
    }

    /** id de l'ancêtre -> nombre de générations (1 = parent, 2 = grand-parent, 3 = arrière-grand-parent). */
    private Map<Long, Integer> ancetres(Map<Long, Lapin> idx, Long id) {
        Map<Long, Integer> res = new HashMap<>();
        List<Long> courant = List.of(id);
        for (int g = 1; g <= 3; g++) {
            List<Long> suivant = new ArrayList<>();
            for (Long x : courant) {
                Lapin l = idx.get(x);
                if (l == null) continue;
                for (Long p : new Long[]{l.getPereId(), l.getMereId()}) {
                    if (p != null) { res.merge(p, g, Math::min); suivant.add(p); }
                }
            }
            courant = suivant;
        }
        return res;
    }
}
