package com.ferme.lapins.service;

import com.ferme.lapins.model.CodeReinitialisation;
import com.ferme.lapins.model.Utilisateur;
import com.ferme.lapins.repository.CodeReinitialisationRepo;
import com.ferme.lapins.repository.UtilisateurRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/** Mot de passe oublié : code à 6 chiffres envoyé par email (SMTP), valable 15 minutes, 5 essais. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReinitialisationService {
    private static final String ERREUR = "Code invalide ou expiré";

    private final UtilisateurRepo users;
    private final CodeReinitialisationRepo codes;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mail;
    private final SecureRandom alea = new SecureRandom();

    @Value("${MAIL_FROM:}") private String expediteur;
    @Value("${spring.mail.username:}") private String compteMail;

    /** Répond toujours de la même façon, que le compte existe ou non (pas de fuite d'information). */
    @Transactional
    public void demander(String emailBrut) {
        String email = emailBrut == null ? "" : emailBrut.trim().toLowerCase();
        Utilisateur u = users.findByEmail(email).orElse(null);
        if (u == null) return;
        CodeReinitialisation ancien = codes.findFirstByUtilisateurIdOrderByIdDesc(u.getId()).orElse(null);
        if (ancien != null && ancien.getCreeLe().isAfter(LocalDateTime.now().minusSeconds(60))) return; // une demande par minute
        codes.deleteByUtilisateurId(u.getId());
        String code = String.format("%06d", alea.nextInt(1_000_000));
        CodeReinitialisation c = new CodeReinitialisation();
        LocalDateTime maintenant = LocalDateTime.now();
        c.setUtilisateurId(u.getId());
        c.setCodeHash(encoder.encode(code));
        c.setCreeLe(maintenant);
        c.setExpiration(maintenant.plusMinutes(15));
        codes.save(c);
        envoyer(u, code);
    }

    private void envoyer(Utilisateur u, String code) {
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP non configuré : code de réinitialisation de {} = {}", u.getEmail(), code);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(u.getEmail());
            String de = expediteur == null || expediteur.isBlank() ? compteMail : expediteur;
            if (de != null && !de.isBlank()) m.setFrom(de);
            m.setSubject("Ferme des Lapins : code de réinitialisation");
            m.setText("Bonjour " + u.getNom() + ",\n\nVotre code de réinitialisation est : " + code
                    + "\nIl est valable 15 minutes.\n\nSi vous n'êtes pas à l'origine de cette demande, ignorez ce message.");
            sender.send(m);
        } catch (Exception e) {
            log.warn("Envoi du mail impossible : {}", e.getMessage());
        }
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void reinitialiser(String emailBrut, String code, String nouveau) {
        String email = emailBrut == null ? "" : emailBrut.trim().toLowerCase();
        Utilisateur u = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException(ERREUR));
        CodeReinitialisation c = codes.findFirstByUtilisateurIdOrderByIdDesc(u.getId()).orElseThrow(() -> new IllegalArgumentException(ERREUR));
        if (c.getExpiration().isBefore(LocalDateTime.now()) || c.getTentatives() >= 5) {
            codes.delete(c);
            throw new IllegalArgumentException(ERREUR);
        }
        if (code == null || !encoder.matches(code.trim(), c.getCodeHash())) {
            c.setTentatives(c.getTentatives() + 1);
            codes.save(c);
            throw new IllegalArgumentException(ERREUR);
        }
        if (nouveau == null || nouveau.length() < 6) throw new IllegalArgumentException("Mot de passe trop court (6 caractères minimum)");
        u.setMotDePasse(encoder.encode(nouveau));
        users.save(u);
        codes.delete(c);
    }
}
