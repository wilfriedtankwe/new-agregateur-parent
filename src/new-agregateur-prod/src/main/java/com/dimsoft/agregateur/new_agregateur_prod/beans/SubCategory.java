package com.dimsoft.agregateur.new_agregateur_prod.beans;

import jakarta.persistence.*;

import java.util.Date;


@Entity
@Table(name = "subcategory")
@NamedQueries({ @NamedQuery(name = "SubCategory.findAll", query = "SELECT s FROM SubCategory s"),
        @NamedQuery(name = "SubCategory.findById", query = "SELECT s FROM SubCategory s WHERE s.id = :id") })
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "title")
    private String title;
    @ManyToOne
    private Category category;
    @Column(name = "comment")
    private String comment;
    @Column(name = "created_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOn;
    @Column(name = "last_update_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateOn;

    public Long getId() {
        return id;
    }

    public void setId(Long subCategoryId) {
        this.id = subCategoryId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

}

