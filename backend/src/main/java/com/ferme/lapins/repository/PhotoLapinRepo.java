package com.ferme.lapins.repository;

import com.ferme.lapins.model.PhotoLapin;
import java.util.Optional;

public interface PhotoLapinRepo extends OwnedRepo<PhotoLapin> {
    Optional<PhotoLapin> findByLapinIdAndUtilisateurId(Long lapinId, Long utilisateurId);
}
