package com.dimsoft.agregateur.new_agregateur_prod.repositories;


import com.dimsoft.agregateur.new_agregateur_prod.beans.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget,Long> {


}



