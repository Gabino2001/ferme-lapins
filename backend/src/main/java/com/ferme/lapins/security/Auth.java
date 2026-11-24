package com.ferme.lapins.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Accès à l'utilisateur connecté (le « principal » est l'id de la ferme, l'utilisateur réel est dans les détails). */
public final class Auth {
    private Auth() {}

    /** Id de la personne connectée (différent de l'id de la ferme pour un employé). */
    public static Long utilisateurId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getDetails() instanceof Long id ? id : null;
    }

    public static String role() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        for (GrantedAuthority g : a.getAuthorities()) {
            if (g.getAuthority().startsWith("ROLE_")) return g.getAuthority().substring(5);
        }
        return null;
    }

    /** Vrai pour le propriétaire, et aussi hors requête (tâches planifiées). */
    public static boolean estProprietaire() {
        String r = role();
        return r == null || "PROPRIETAIRE".equals(r);
    }
}
