package com.ferme.lapins.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;

@NoRepositoryBean
public interface OwnedRepo<T> extends JpaRepository<T, Long> {
    List<T> findByUtilisateurId(Long utilisateurId);
}
