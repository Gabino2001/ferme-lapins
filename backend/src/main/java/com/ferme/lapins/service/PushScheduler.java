package com.ferme.lapins.service;

import com.ferme.lapins.dto.Alerte;
import com.ferme.lapins.model.Appareil;
import com.ferme.lapins.repository.AppareilRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Résumé quotidien envoyé sur les téléphones (par défaut à 7 h, heure de Douala). */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushScheduler {
    private final AppareilRepo appareils;
    private final AlerteService alertes;
    private final PushService push;

    @Scheduled(cron = "${app.push.cron:0 0 7 * * *}", zone = "${app.push.zone:Africa/Douala}")
    public void resumeQuotidien() {
        if (!push.actif()) return;
        Map<Long, List<Appareil>> parUtilisateur = appareils.findAll().stream()
                .collect(Collectors.groupingBy(Appareil::getUtilisateurId));
        parUtilisateur.forEach((uid, liste) -> {
            try {
                List<Alerte> al = alertes.calculer(uid).stream().filter(a -> !"blu".equals(a.severite()) && !"argent".equals(a.section()) && !"clients".equals(a.section())).toList();
                if (al.isEmpty()) return;
                String titre = al.size() + (al.size() > 1 ? " points à traiter à la ferme" : " point à traiter à la ferme");
                String corps = al.get(0).titre() + (al.size() > 1 ? " et " + (al.size() - 1) + " autre(s)" : "");
                for (Appareil a : liste) {
                    if (!push.envoyer(a.getToken(), titre, corps)) appareils.delete(a);
                }
            } catch (Exception e) {
                log.warn("Résumé push impossible pour l'utilisateur {} : {}", uid, e.getMessage());
            }
        });
    }
}
