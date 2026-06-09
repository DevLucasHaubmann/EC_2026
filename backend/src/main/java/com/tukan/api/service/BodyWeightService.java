package com.tukan.api.service;

import com.tukan.api.dto.bodyweight.BodyWeightLogResponse;
import com.tukan.api.dto.bodyweight.BodyWeightSummaryResponse;
import com.tukan.api.entity.BodyWeightLog;
import com.tukan.api.entity.User;
import com.tukan.api.repository.BodyWeightLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BodyWeightService {

    private final BodyWeightLogRepository bodyWeightLogRepository;
    private final UserService userService;

    @Transactional
    public BodyWeightLogResponse register(String email, BigDecimal weightKg) {
        User user = userService.findByEmail(email);

        // Every registration adds a new history entry — no upsert. Recording twice on the
        // same day keeps both measurements so the user sees the real progression.
        BodyWeightLog log = new BodyWeightLog();
        log.setUser(user);
        log.setMeasuredDate(LocalDate.now());
        log.setWeightKg(weightKg);

        return BodyWeightLogResponse.from(bodyWeightLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<BodyWeightLogResponse> getHistory(String email) {
        User user = userService.findByEmail(email);
        return bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(user.getId())
                .stream()
                .map(BodyWeightLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BodyWeightSummaryResponse getSummary(String email) {
        User user = userService.findByEmail(email);
        List<BodyWeightLog> history = bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(user.getId());

        if (history.isEmpty()) {
            return new BodyWeightSummaryResponse(null, null, null, null, 0, List.of());
        }

        List<BodyWeightLogResponse> historyDtos = history.stream()
                .map(BodyWeightLogResponse::from)
                .toList();

        BodyWeightLog first = history.get(0);
        BodyWeightLog last = history.get(history.size() - 1);

        if (history.size() == 1) {
            return new BodyWeightSummaryResponse(
                    first.getWeightKg(),
                    first.getWeightKg(),
                    null,
                    null,
                    1,
                    historyDtos
            );
        }

        BigDecimal initialWeight = first.getWeightKg();
        BigDecimal currentWeight = last.getWeightKg();
        BigDecimal totalChange = currentWeight.subtract(initialWeight);
        String trend = computeTrend(initialWeight, currentWeight);

        return new BodyWeightSummaryResponse(
                currentWeight,
                initialWeight,
                totalChange,
                trend,
                history.size(),
                historyDtos
        );
    }

    /**
     * Determines weight trend direction based on initial vs current weight.
     * Uses compareTo to avoid floating-point equality pitfalls with BigDecimal.
     */
    private String computeTrend(BigDecimal initialWeight, BigDecimal currentWeight) {
        int comparison = currentWeight.compareTo(initialWeight);
        if (comparison == 0) {
            return "STABLE";
        }
        return comparison < 0 ? "LOSS" : "GAIN";
    }
}
