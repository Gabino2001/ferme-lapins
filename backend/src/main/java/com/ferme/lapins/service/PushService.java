package com.ferme.lapins.service;

import org.springframework.stereotype.Service;

/**
 * Variante légère : les notifications push (Firebase) sont désactivées.
 * L'API reste la même, pour pouvoir remettre la version complète plus tard.
 */
@Service
public class PushService {
    public boolean actif() { return false; }

    /** Retourne false si le jeton n'est plus valide : jamais le cas ici. */
    public boolean envoyer(String token, String titre, String corps) { return true; }
}
