package com.tukan.api.controller;

import com.tukan.api.dto.evolution.DailyEvolutionMetrics;
import com.tukan.api.dto.evolution.WeeklyEvolutionSummary;
import com.tukan.api.service.EvolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class EvolutionController {

    private final EvolutionService evolutionService;

    @GetMapping("/evolution/daily")
    public ResponseEntity<DailyEvolutionMetrics> getDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        List<DailyEvolutionMetrics> metrics = evolutionService.getDailyMetrics(
                authentication.getName(), date, date);
        // getDailyMetrics with start==end always returns exactly one element
        return ResponseEntity.ok(metrics.get(0));
    }

    @GetMapping("/evolution/weekly")
    public ResponseEntity<WeeklyEvolutionSummary> getWeekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication authentication) {
        WeeklyEvolutionSummary summary = evolutionService.getSummary(
                authentication.getName(), weekStart);
        return ResponseEntity.ok(summary);
    }
}
