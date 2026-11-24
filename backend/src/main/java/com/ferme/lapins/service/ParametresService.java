package com.ferme.lapins.service;

import com.ferme.lapins.repository.UtilisateurRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Paramètres de la ferme, portés par le compte du propriétaire. */
@Service
@RequiredArgsConstructor
public class ParametresService {
    private final UtilisateurRepo users;

    public String devise(Long fermeId) {
        return users.findById(fermeId)
                .map(u -> u.getDevise() == null || u.getDevise().isBlank() ? "FCFA" : u.getDevise())
                .orElse("FCFA");
    }
}
