package com.dimsoft.agregateur.new_agregateur_prod.beans;



import jakarta.persistence.*;

import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "compte")
@NamedQueries({ @NamedQuery(name = "Compte.findAll", query = "SELECT c FROM Compte c"),
        @NamedQuery(name = "Compte.findById", query = "SELECT c FROM Compte c WHERE c.id = :id") })
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "title")
    private String title;
    @ManyToOne
    private Bank bank;
    @Column(name = "type")
    private String type;
    @Column(name = "comment")
    private String comment;
    @Column(name = "created_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOn;
    @Column(name = "last_update_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateOn;

    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long compteId) {
        this.id = compteId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Date getLastUpdateOn() {
        return lastUpdateOn;
    }

    public void setLastUpdateOn(Date lastUpdateOn) {
        this.lastUpdateOn = lastUpdateOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Compte compte = (Compte) o;
        return id.equals(compte.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Compte{" + "id=" + id + ", title='" + title + '\'' + '}';
    }
}
