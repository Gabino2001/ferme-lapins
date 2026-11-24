package com.ferme.lapins.repository;

import com.ferme.lapins.model.Appareil;
import java.util.Optional;

public interface AppareilRepo extends OwnedRepo<Appareil> {
    Optional<Appareil> findByToken(String token);
}
