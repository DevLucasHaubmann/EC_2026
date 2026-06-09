package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_active_day",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_active_day", columnNames = {"user_id", "active_date"}),
        indexes = @Index(name = "idx_user_active_day_user", columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
public class UserActiveDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "active_date", nullable = false)
    private LocalDate activeDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
