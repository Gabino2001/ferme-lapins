package com.ferme.lapins.service;

import com.ferme.lapins.dto.Croissance;
import com.ferme.lapins.event.FermeChanged;
import com.ferme.lapins.model.LotEngraissement;
import com.ferme.lapins.model.PeseeLot;
import com.ferme.lapins.repository.LotEngraissementRepo;
import com.ferme.lapins.repository.PeseeLotRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import static java.time.temporal.ChronoUnit.DAYS;

/** Courbe de croissance d'un lot d'engraissement et aide à la décision de vente. */
@Service
@RequiredArgsConstructor
@Transactional
public class CroissanceService {
    private final LotEngraissementRepo lots;
    private final PeseeLotRepo pesees;
    private final ApplicationEventPublisher events;

    private LotEngraissement lot(Long uid, Long id) {
        return lots.findById(id).filter(l -> uid.equals(l.getUtilisateurId()))
                .orElseThrow(() -> new NoSuchElementException("Lot introuvable"));
    }

    @Transactional(readOnly = true)
    public Croissance courbe(Long uid, Long lotId) {
        LotEngraissement lot = lot(uid, lotId);
        return calculer(lot.getDateNaissance(), pesees.findByLotIdAndUtilisateurIdOrderByDate(lotId, uid));
    }

    public Croissance peser(Long uid, Long lotId, double poids, LocalDate date) {
        if (poids <= 0 || poids > 10) throw new IllegalArgumentException("Poids invalide (en kg, par lapin)");
        lot(uid, lotId);
        PeseeLot p = new PeseeLot();
        p.setUtilisateurId(uid);
        p.setLotId(lotId);
        p.setPoidsMoyen(poids);
        p.setDate(date == null ? LocalDate.now() : date);
        pesees.save(p);
        events.publishEvent(new FermeChanged(uid));
        return courbe(uid, lotId);
    }

    private static double arrondi(double x) { return Math.round(x * 10) / 10.0; }

    public static Croissance calculer(LocalDate naissance, List<PeseeLot> ps) {
        List<Croissance.Point> points = new ArrayList<>();
        for (PeseeLot p : ps) {
            points.add(new Croissance.Point(p.getDate(), p.getPoidsMoyen(), naissance == null ? null : (int) DAYS.between(naissance, p.getDate())));
        }
        Double parJour = null, derniere = null;
        String conseil = null;
        if (ps.size() >= 2) {
            PeseeLot premiere = ps.get(0), derniereP = ps.get(ps.size() - 1), avant = ps.get(ps.size() - 2);
            long jTotal = DAYS.between(premiere.getDate(), derniereP.getDate());
            if (jTotal > 0) parJour = arrondi((derniereP.getPoidsMoyen() - premiere.getPoidsMoyen()) * 1000 / jTotal);
            long jDerniere = DAYS.between(avant.getDate(), derniereP.getDate());
            if (jDerniere > 0) {
                double g = (derniereP.getPoidsMoyen() - avant.getPoidsMoyen()) * 1000 / jDerniere;
                derniere = arrondi(g);
                if (ps.size() >= 3) {
                    PeseeLot avant2 = ps.get(ps.size() - 3);
                    long jPrec = DAYS.between(avant2.getDate(), avant.getDate());
                    if (jPrec > 0) {
                        double gPrec = (avant.getPoidsMoyen() - avant2.getPoidsMoyen()) * 1000 / jPrec;
                        if (gPrec > 0 && g < gPrec * 0.6) {
                            conseil = "La croissance ralentit nettement. Si les lapins ont atteint le poids demandé par vos clients, "
                                    + "c’est un bon moment pour vendre : au-delà, l’aliment coûte souvent plus qu’il ne rapporte.";
                        }
                    }
                }
            }
        }
        return new Croissance(points, parJour, derniere, conseil);
    }
}
