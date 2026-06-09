package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "body_weight_log",
        indexes = @Index(name = "idx_body_weight_log_user", columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
public class BodyWeightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "measured_date", nullable = false)
    private LocalDate measuredDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
