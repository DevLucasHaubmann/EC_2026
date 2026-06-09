package com.tukan.api.dto.bodyweight;

import com.tukan.api.entity.BodyWeightLog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BodyWeightLogResponse(
        Long id,
        BigDecimal weightKg,
        LocalDate measuredDate,
        Instant createdAt
) {

    public static BodyWeightLogResponse from(BodyWeightLog log) {
        return new BodyWeightLogResponse(
                log.getId(),
                log.getWeightKg(),
                log.getMeasuredDate(),
                log.getCreatedAt()
        );
    }
}
