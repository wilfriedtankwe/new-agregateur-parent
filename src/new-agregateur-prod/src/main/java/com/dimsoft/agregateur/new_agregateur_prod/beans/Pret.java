package com.dimsoft.agregateur.new_agregateur_prod.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pret")
public class Pret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "dateDebut")
    private Date dateDebut;
    @Column(name = "dateFin")
    private Date dateFin;
    @Column(name = "datePrelevement")
    private Date datePrelevement;
    @Column(name = "libelle")
    private String libelle;
    @Column(name = "cmt")
    private String cmt;
    @Column(name = "montantEmprunte")
    private Float montantEmprunte;
    @Column(name = "teg")
    private Float teg;
    @Column(name = "taeg")
    private Float taeg;
    @Column(name = "coutAssurance")
    private Float coutAssurance;
    @Column(name = "dureePretenMois")
    private Float dureePretenMois;
    @ManyToOne
    private Compte compte;
    @Column(name = "created_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOn;
    @Column(name = "last_update_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateOn;
    @Column(name = "somme_totale_empruntee")
    private Float sommeTotaleEmpruntee;
    @Column(name = "historique_montant")
    @Lob
    private String historiqueMontant;
    @OneToMany(mappedBy = "pret", cascade = CascadeType.REMOVE)
    private List<Budget> budgets = new ArrayList<>();
    @Column(name = "echeancier_facture", nullable = true)
    private String echeancierFacture;
    @Column(name = "echeancier_avoir", nullable = true)
    private String echeancierAvoir;
}

