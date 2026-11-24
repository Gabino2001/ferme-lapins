package com.ferme.lapins.service;

import com.ferme.lapins.dto.Alerte;
import com.ferme.lapins.dto.Bilan;
import com.ferme.lapins.dto.Dashboard;
import com.ferme.lapins.dto.Statistiques;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Variante légère : le rapport est une page HTML. Le bouton « Imprimer / Enregistrer en PDF » ouvre la boîte
 * d'impression du navigateur, qui sait créer un PDF.
 */
@Service
@RequiredArgsConstructor
public class ExportPdfService {
    private final DashboardService dashboard;
    private final BilanService bilans;
    private final StatistiquesService statistiques;
    private final ParametresService parametres;

    private static String h(Object o) {
        return String.valueOf(o).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public byte[] generer(Long uid) {
        final String devise = parametres.devise(uid);
        DoubleFunction<String> argent = x -> String.format(Locale.ROOT, "%,.0f", x).replace(',', ' ') + " " + devise;
        Dashboard d = dashboard.dashboard(uid);
        Bilan b = bilans.calculer(uid, 30);
        Statistiques s = statistiques.calculer(uid);

        StringBuilder x = new StringBuilder();
        x.append("""
                <!DOCTYPE html><html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Rapport de la ferme</title>
                <style>
                body{font-family:Arial,Helvetica,sans-serif;max-width:800px;margin:24px auto;padding:0 16px;color:#17261E}
                h1{margin-bottom:0}h2{margin-top:28px;border-bottom:2px solid #2F6B4F;padding-bottom:4px}
                table{width:100%;border-collapse:collapse;margin-top:8px}td,th{border:1px solid #bbb;padding:6px 8px;text-align:left}
                td.n,th.n{text-align:right}.gris{color:#666}.bouton{padding:10px 16px;font-size:16px;background:#2F6B4F;color:#fff;border:0;border-radius:8px;cursor:pointer}
                @media print{.bouton{display:none}}
                </style></head><body>
                <button class="bouton" onclick="window.print()">Imprimer / Enregistrer en PDF</button>
                <h1>Rapport de la ferme</h1>
                """);
        x.append("<p class=\"gris\">Édité le ").append(LocalDate.now()).append("</p>");

        x.append("<h2>Situation</h2><p><b>").append(h(d.statut())).append("</b> (score ").append(d.score()).append("/100) · ").append(h(d.resume())).append("</p><table>");
        String[][] libelles = {{"lapins", "Lapins"}, {"gestantes", "Lapines gestantes"}, {"sousLaMere", "Lapereaux sous la mère"},
                {"engraissement", "Lapereaux en engraissement"}, {"malades", "Lapins malades"}, {"cagesLibres", "Cages libres"}};
        for (String[] l : libelles) x.append("<tr><td>").append(l[1]).append("</td><td class=\"n\">").append(d.kpis().get(l[0])).append("</td></tr>");
        x.append("</table>");

        x.append("<h2>Alertes en cours</h2>");
        if (d.alertes().isEmpty()) x.append("<p>Aucune alerte.</p>");
        else {
            x.append("<ul>");
            for (Alerte a : d.alertes()) x.append("<li><b>").append(h(a.titre())).append("</b> : ").append(h(a.detail())).append("</li>");
            x.append("</ul>");
        }

        x.append("<h2>Bilan des 30 derniers jours</h2><table>");
        ligne(x, "Ventes", argent.apply(b.ventes()));
        ligne(x, "Dépenses", argent.apply(b.depenses()));
        ligne(x, b.resultat() >= 0 ? "Gain" : "Perte", argent.apply(Math.abs(b.resultat())));
        ligne(x, "Lapins vendus", String.valueOf(b.lapinsVendus()));
        ligne(x, "Coût de revient par lapin vendu", argent.apply(b.coutRevient()));
        ligne(x, "Prix de vente moyen", argent.apply(b.prixMoyen()));
        x.append("</table>");

        if (!b.depensesParPoste().isEmpty()) {
            x.append("<h2>Dépenses par poste</h2><table>");
            for (Map.Entry<String, Double> e : b.depensesParPoste().entrySet()) ligne(x, e.getKey(), argent.apply(e.getValue()));
            x.append("</table>");
        }

        x.append("<h2>Reproduction</h2><p>").append(s.nbPortees()).append(" portées · ").append(s.tailleMoyennePortee())
                .append(" nés par portée en moyenne · ").append(s.vivantsMoyens()).append(" vivants · mortalité avant sevrage ")
                .append(s.mortaliteSevrage()).append(" %</p>");
        if (!s.classement().isEmpty()) {
            x.append("<table><tr><th>Lapine</th><th class=\"n\">Portées</th><th class=\"n\">Nés (moyenne)</th><th class=\"n\">Sevrés</th></tr>");
            for (Statistiques.LapineStat l : s.classement()) {
                x.append("<tr><td>").append(h(l.nom())).append("</td><td class=\"n\">").append(l.portees()).append("</td><td class=\"n\">")
                        .append(l.moyenneNes()).append("</td><td class=\"n\">").append(l.sevres()).append("</td></tr>");
            }
            x.append("</table>");
        }
        x.append("</body></html>");
        return x.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void ligne(StringBuilder x, String libelle, String valeur) {
        x.append("<tr><td>").append(h(libelle)).append("</td><td class=\"n\">").append(h(valeur)).append("</td></tr>");
    }
}
