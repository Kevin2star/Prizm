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
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "members", indexes = {
        @Index(name = "idx_members_space_id", columnList = "space_id")
})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(nullable = false, length = 80)
    private String nickname;

    @Column(nullable = false, length = 120)
    private String school;

    @Column(nullable = false, length = 120)
    private String major;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Member() {
    }

    public Member(Space space, String nickname, String school, String major) {
        this.space = space;
        this.nickname = nickname;
        this.school = school;
        this.major = major;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Space getSpace() {
        return space;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSchool() {
        return school;
    }

    public String getMajor() {
        return major;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
