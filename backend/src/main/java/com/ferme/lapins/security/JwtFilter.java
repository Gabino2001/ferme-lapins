package com.ferme.lapins.security;

import com.ferme.lapins.model.Utilisateur;
import com.ferme.lapins.repository.UtilisateurRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/**
 * Le jeton contient l'id de la personne. Son rôle et sa ferme sont relus en base à chaque requête :
 * un employé retiré ou un changement de rôle s'applique immédiatement.
 * Principal = id de la ferme (le propriétaire) ; détails = id de la personne connectée.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UtilisateurRepo users;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() { return false; } // nécessaire pour le flux SSE

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                Utilisateur u = users.findById(jwt.lire(h.substring(7))).orElse(null);
                if (u != null) {
                    Long ferme = u.getProprietaireId() != null ? u.getProprietaireId() : u.getId();
                    String role = u.getRole() == null ? "PROPRIETAIRE" : u.getRole();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            ferme, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    auth.setDetails(u.getId());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) { /* jeton invalide : requête anonyme -> 401 */ }
        }
        chain.doFilter(req, res);
    }
}
