package com.ferme.lapins.repository;

import com.ferme.lapins.model.CodeReinitialisation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CodeReinitialisationRepo extends JpaRepository<CodeReinitialisation, Long> {
    Optional<CodeReinitialisation> findFirstByUtilisateurIdOrderByIdDesc(Long utilisateurId);
    void deleteByUtilisateurId(Long utilisateurId);
}
