package com.dimsoft.agregateur.new_agregateur_prod.controller;

import com.dimsoft.agregateur.new_agregateur_prod.services.AggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aggregation")
public class AggregationController {

    @Autowired
    private AggregationService aggregationService;

    @PostMapping("/demarrer")
    public ResponseEntity<AggregationService.AggregationGlobaleResultDto> demarrerAggregation() {
        AggregationService.AggregationGlobaleResultDto resultat = aggregationService.demarrerAggregation();
        return ResponseEntity.ok(resultat);
    }
}
