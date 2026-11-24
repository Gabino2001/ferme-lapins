package com.ferme.lapins.repository;

import com.ferme.lapins.model.PeseeLot;
import java.util.List;

public interface PeseeLotRepo extends OwnedRepo<PeseeLot> {
    List<PeseeLot> findByLotIdAndUtilisateurIdOrderByDate(Long lotId, Long utilisateurId);
}
