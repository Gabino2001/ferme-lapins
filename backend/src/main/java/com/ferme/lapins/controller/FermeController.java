package com.ferme.lapins.controller;

import com.ferme.lapins.dto.Alerte;
import com.ferme.lapins.dto.Bilan;
import com.ferme.lapins.dto.CageEtat;
import com.ferme.lapins.dto.Dashboard;
import com.ferme.lapins.dto.Dtos;
import com.ferme.lapins.model.CasMaladie;
import com.ferme.lapins.model.Guide;
import com.ferme.lapins.model.LotEngraissement;
import com.ferme.lapins.model.Mouvement;
import com.ferme.lapins.model.Pesee;
import com.ferme.lapins.model.Portee;
import com.ferme.lapins.model.Saillie;
import com.ferme.lapins.model.StockItem;
import com.ferme.lapins.security.Auth;
import com.ferme.lapins.service.AlerteService;
import com.ferme.lapins.service.AlerteStream;
import com.ferme.lapins.service.BilanService;
import com.ferme.lapins.service.CageService;
import com.ferme.lapins.service.DashboardService;
import com.ferme.lapins.service.FermeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FermeController {
    private final FermeService ferme;
    private final DashboardService dashboard;
    private final AlerteService alertes;
    private final AlerteStream stream;
    private final CageService cages;
    private final BilanService bilans;

    @GetMapping("/dashboard") public Dashboard dashboard(@AuthenticationPrincipal Long uid) { return dashboard.dashboard(uid); }
    @GetMapping("/alertes") public List<Alerte> alertes(@AuthenticationPrincipal Long uid) {
        return alertes.calculer(uid).stream()
                .filter(a -> Auth.estProprietaire() || !List.of("argent", "clients").contains(a.section())).toList();
    }
    @GetMapping(value = "/alertes/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal Long uid) { return stream.subscribe(uid); }
    @GetMapping("/cages/plan") public List<CageEtat> plan(@AuthenticationPrincipal Long uid) { return cages.plan(uid); }
    @GetMapping("/bilan") public Bilan bilan(@AuthenticationPrincipal Long uid, @RequestParam(defaultValue = "30") int jours) { return bilans.calculer(uid, jours); }
    @GetMapping("/maladies/guide") public List<Guide.Maladie> guide() { return Guide.LISTE; }

    @PostMapping("/saillies/{id}/palpation")
    public Saillie palpation(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestParam String resultat) { return ferme.palpation(uid, id, resultat); }
    @PostMapping("/saillies/{id}/mise-bas")
    public Portee miseBas(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody Dtos.MiseBas d) { return ferme.miseBas(uid, id, d); }
    @PostMapping("/portees/{id}/sevrage")
    public LotEngraissement sevrage(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody Dtos.Sevrage d) { return ferme.sevrage(uid, id, d); }
    @PostMapping("/lots/{id}/vente")
    public Mouvement vente(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody Dtos.VenteLot d) { return ferme.venteLot(uid, id, d); }
    @PostMapping("/lots/{id}/perte")
    public void perte(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody Dtos.PerteLot d) { ferme.perteLot(uid, id, d.nombre()); }
    @GetMapping("/lapins/{id}/pesees")
    public List<Pesee> pesees(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return ferme.pesees(uid, id); }
    @PostMapping("/lapins/{id}/pesees")
    public Pesee pesee(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestBody Dtos.PeseeReq d) { return ferme.pesee(uid, id, d); }
    @PostMapping("/maladies/{id}/statut")
    public CasMaladie statut(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestParam String statut) { return ferme.statutCas(uid, id, statut); }
    @PostMapping("/stock/{id}/reappro")
    public StockItem reappro(@AuthenticationPrincipal Long uid, @PathVariable Long id, @RequestParam double quantite) { return ferme.reappro(uid, id, quantite); }
}
