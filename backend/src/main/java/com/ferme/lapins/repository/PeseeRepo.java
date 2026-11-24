package com.ferme.lapins.repository;

import com.ferme.lapins.model.Pesee;
import java.util.List;

public interface PeseeRepo extends OwnedRepo<Pesee> {
    List<Pesee> findByLapinIdAndUtilisateurIdOrderByDate(Long lapinId, Long utilisateurId);
}
