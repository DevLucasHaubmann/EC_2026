package com.tukan.api.controller;

import com.tukan.api.dto.meallog.LogMealRequest;
import com.tukan.api.dto.meallog.MealLogResponse;
import com.tukan.api.service.MealLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class MealLogController {

    private final MealLogService mealLogService;

    @PostMapping("/meal-logs")
    public ResponseEntity<MealLogResponse> logMeal(
            @Valid @RequestBody LogMealRequest request,
            Authentication authentication) {
        MealLogResponse response = mealLogService.logMeal(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/meal-logs")
    public ResponseEntity<List<MealLogResponse>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        List<MealLogResponse> responses = mealLogService.getLogsByDate(authentication.getName(), date);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/meal-logs/{id}")
    public ResponseEntity<Void> deleteLog(
            @PathVariable Long id,
            Authentication authentication) {
        mealLogService.deleteLog(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
