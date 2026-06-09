package com.tukan.api.dto.bodyweight;

import java.math.BigDecimal;
import java.util.List;

public record BodyWeightSummaryResponse(
        BigDecimal currentWeightKg,
        BigDecimal initialWeightKg,
        BigDecimal totalChangeKg,
        String trend,
        int entryCount,
        List<BodyWeightLogResponse> history
) {
}
