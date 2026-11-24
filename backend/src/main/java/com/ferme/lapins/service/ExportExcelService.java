package com.ferme.lapins.service;

import com.ferme.lapins.model.*;
import com.ferme.lapins.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Variante légère : une archive ZIP contenant un fichier CSV par domaine (lapins, portées, argent...).
 * Les fichiers s'ouvrent directement dans Excel (UTF-8, séparateur point-virgule).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportExcelService {
    private final LapinRepo lapins;
    private final PorteeRepo portees;
    private final LotEngraissementRepo lots;
    private final CasMaladieRepo cas;
    private final SoinRepo soins;
    private final MouvementRepo mouvements;
    private final CommandeRepo commandes;
    private final ClientRepo clients;
    private final StockItemRepo stock;

    public byte[] generer(Long uid) {
        Map<Long, String> noms = new HashMap<>();
        List<Lapin> ls = lapins.findByUtilisateurId(uid);
        ls.forEach(l -> noms.put(l.getId(), l.getNom()));
        Map<Long, String> nomsClients = new HashMap<>();
        clients.findByUtilisateurId(uid).forEach(c -> nomsClients.put(c.getId(), c.getNom()));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            List<Object[]> l1 = new ArrayList<>();
            for (Lapin l : ls) l1.add(new Object[]{l.getNom(), l.getSexe(), l.getRace(), l.getCage(), l.getDateNaissance(), l.getPoids(),
                    l.getPereId() != null ? noms.get(l.getPereId()) : l.getPere(), l.getMereId() != null ? noms.get(l.getMereId()) : l.getMere(), l.getNotes()});
            fichier(zip, "lapins.csv", new String[]{"Nom", "Sexe", "Race", "Cage", "Naissance", "Poids (kg)", "Père", "Mère", "Notes"}, l1);

            List<Object[]> l2 = new ArrayList<>();
            for (Portee p : portees.findByUtilisateurId(uid)) l2.add(new Object[]{noms.get(p.getMereId()), p.getDate(), p.getNes(), p.getVivants(), p.getSevres(), Boolean.TRUE.equals(p.getSevree()) ? "oui" : "non"});
            fichier(zip, "portees.csv", new String[]{"Mère", "Date", "Nés", "Vivants", "Sevrés", "Sevrée"}, l2);

            List<Object[]> l3 = new ArrayList<>();
            for (LotEngraissement l : lots.findByUtilisateurId(uid)) l3.add(new Object[]{l.getCage(), l.getNombre(), noms.get(l.getMereId()), l.getDateNaissance()});
            fichier(zip, "engraissement.csv", new String[]{"Cage", "Nombre", "Mère", "Naissance"}, l3);

            List<Object[]> l4 = new ArrayList<>();
            for (CasMaladie c : cas.findByUtilisateurId(uid)) l4.add(new Object[]{noms.get(c.getLapinId()), c.getMaladie(), c.getDate(), c.getStatut()});
            fichier(zip, "maladies.csv", new String[]{"Lapin", "Maladie", "Date", "Statut"}, l4);

            List<Object[]> l5 = new ArrayList<>();
            for (Soin s : soins.findByUtilisateurId(uid)) l5.add(new Object[]{s.getType(), s.getNom(), s.getLapinId() == null ? "Tout le cheptel" : noms.get(s.getLapinId()), s.getDatePrevue(), s.getDateRealisee()});
            fichier(zip, "soins.csv", new String[]{"Type", "Nom", "Pour", "Prévu le", "Fait le"}, l5);

            List<Object[]> l6 = new ArrayList<>();
            for (Mouvement m : mouvements.findByUtilisateurId(uid)) l6.add(new Object[]{m.getDate(), m.getType(), m.getCategorie(), m.getQuantite(), m.getMontant()});
            fichier(zip, "argent.csv", new String[]{"Date", "Type", "Catégorie", "Quantité", "Montant"}, l6);

            List<Object[]> l7 = new ArrayList<>();
            for (Commande c : commandes.findByUtilisateurId(uid)) l7.add(new Object[]{nomsClients.get(c.getClientId()), c.getProduit(), c.getQuantite(), c.getPrixUnitaire(), c.getMontantPaye(), c.getDateCommande(), c.getDateLivraisonPrevue(), c.getStatut()});
            fichier(zip, "commandes.csv", new String[]{"Client", "Produit", "Quantité", "Prix unitaire", "Payé", "Commande", "Livraison prévue", "Statut"}, l7);

            List<Object[]> l8 = new ArrayList<>();
            for (StockItem k : stock.findByUtilisateurId(uid)) l8.add(new Object[]{k.getNom(), k.getQuantite(), k.getUnite(), k.getConsoJour()});
            fichier(zip, "stock.csv", new String[]{"Article", "Quantité", "Unité", "Consommation par jour"}, l8);

            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Export impossible : " + e.getMessage(), e);
        }
    }

    private void fichier(ZipOutputStream zip, String nom, String[] entetes, List<Object[]> lignes) throws IOException {
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM : Excel reconnaît l'UTF-8 (accents)
        sb.append(String.join(";", entetes)).append("\r\n");
        for (Object[] l : lignes) {
            for (int i = 0; i < l.length; i++) {
                if (i > 0) sb.append(';');
                sb.append(cellule(l[i]));
            }
            sb.append("\r\n");
        }
        zip.putNextEntry(new ZipEntry(nom));
        zip.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String cellule(Object v) {
        if (v == null) return "";
        String s = v instanceof Double d ? d.toString().replace('.', ',') : v.toString();
        return s.contains(";") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
