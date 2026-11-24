package com.ferme.lapins.controller;

import com.ferme.lapins.model.Utilisateur;
import com.ferme.lapins.repository.UtilisateurRepo;
import com.ferme.lapins.security.Auth;
import com.ferme.lapins.security.JwtService;
import com.ferme.lapins.service.ExportExcelService;
import com.ferme.lapins.service.ExportPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Exports réservés au propriétaire (variante légère) :
 *  - type=excel : archive ZIP de fichiers CSV, qui s'ouvrent dans Excel ;
 *  - type=pdf   : rapport HTML à imprimer ou enregistrer en PDF depuis le navigateur.
 * Le téléchargement se fait par un lien à jeton court (5 minutes).
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {
    private final JwtService jwt;
    private final UtilisateurRepo users;
    private final ExportExcelService excel;
    private final ExportPdfService pdf;

    @PostMapping("/lien")
    public Map<String, String> lien(@RequestParam String type) {
        if (!List.of("excel", "pdf").contains(type)) throw new IllegalArgumentException("Type d’export inconnu (excel ou pdf)");
        Long moi = Auth.utilisateurId();
        if (moi == null) throw new IllegalArgumentException("Utilisateur inconnu");
        return Map.of("url", "/api/export/telecharger?type=" + type + "&t=" + jwt.genererExport(moi, 5));
    }

    @GetMapping("/telecharger")
    public ResponseEntity<byte[]> telecharger(@RequestParam String type, @RequestParam String t) {
        Utilisateur u;
        try {
            u = users.findById(jwt.lireExport(t)).orElseThrow();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Lien expiré : relancez l’export depuis l’application");
        }
        if (u.getProprietaireId() != null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé au propriétaire");
        if ("excel".equals(type)) {
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ferme-lapins-" + LocalDate.now() + ".zip\"")
                    .body(excel.generer(u.getId()));
        }
        if ("pdf".equals(type)) {
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                    .body(pdf.generer(u.getId()));
        }
        throw new IllegalArgumentException("Type d’export inconnu (excel ou pdf)");
    }
}
