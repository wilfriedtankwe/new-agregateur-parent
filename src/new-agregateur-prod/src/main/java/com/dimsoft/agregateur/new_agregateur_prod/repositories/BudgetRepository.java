package com.dimsoft.agregateur.new_agregateur_prod.repositories;


import com.dimsoft.agregateur.new_agregateur_prod.beans.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget,Long> {
    @Query("SELECT b FROM Budget b WHERE " +
            "b.dateOperation = ?1 AND " +
            "b.libelle = ?2 AND " +
            "b.montant = ?3 AND " +
            "b.compte.id = ?4 AND " +
            "b.bank.id = ?5")
    Optional<Budget> chercherDoublon(Date date, String libelle, Double montant, Long compteId, Long bankId);
}



