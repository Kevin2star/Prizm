package com.prizm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "artifact_groups", indexes = {
        @Index(name = "idx_groups_space_id", columnList = "space_id")
})
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String commonPoints;

    @Column(columnDefinition = "TEXT")
    private String differences;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Group() {
    }

    public Group(Space space, String label) {
        this.space = space;
        this.label = label;
    }

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Space getSpace() {
        return space;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCommonPoints() {
        return commonPoints;
    }

    public void setCommonPoints(String commonPoints) {
        this.commonPoints = commonPoints;
    }

    public String getDifferences() {
        return differences;
    }

    public void setDifferences(String differences) {
        this.differences = differences;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
