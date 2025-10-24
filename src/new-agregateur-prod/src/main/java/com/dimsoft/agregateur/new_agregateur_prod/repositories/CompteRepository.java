package com.dimsoft.agregateur.new_agregateur_prod.repositories;

import com.dimsoft.agregateur.new_agregateur_prod.beans.Compte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompteRepository extends JpaRepository<Compte,Long> {
}
