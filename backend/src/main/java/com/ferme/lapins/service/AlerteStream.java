package com.ferme.lapins.service;

import com.ferme.lapins.event.FermeChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Temps réel : chaque changement de données pousse le tableau de bord aux appareils connectés. */
@Component
@RequiredArgsConstructor
public class AlerteStream {
    private final DashboardService dashboard;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long uid) {
        SseEmitter e = new SseEmitter(0L);
        List<SseEmitter> list = emitters.computeIfAbsent(uid, k -> new CopyOnWriteArrayList<>());
        list.add(e);
        Runnable retirer = () -> list.remove(e);
        e.onCompletion(retirer);
        e.onTimeout(retirer);
        e.onError(x -> retirer.run());
        try { e.send(SseEmitter.event().name("ready").data("ok")); } catch (Exception ignored) { retirer.run(); }
        return e;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onChange(FermeChanged ev) {
        try { push(ev.utilisateurId()); } catch (Exception ignored) { /* ne jamais casser la requête */ }
    }

    public void push(Long uid) {
        List<SseEmitter> list = emitters.get(uid);
        if (list == null || list.isEmpty()) return;
        String d = "maj"; // simple signal : chaque appareil recharge ses données avec ses propres droits
        for (SseEmitter e : list) {
            try { e.send(SseEmitter.event().name("dashboard").data(d)); }
            catch (Exception x) { list.remove(e); }
        }
    }

    @Scheduled(fixedRate = 25_000)
    public void ping() {
        emitters.values().forEach(list -> list.forEach(e -> {
            try { e.send(SseEmitter.event().comment("ping")); } catch (Exception x) { list.remove(e); }
        }));
    }

    @Scheduled(cron = "0 0 * * * *")
    public void recalculHoraire() {
        emitters.keySet().forEach(id -> { try { push(id); } catch (Exception ignored) { } });
    }
}
