package com.tukan.api.controller;

import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealPlanRecomendacaoResponse;
import com.tukan.api.service.mealplan.MealPlanAiService;
import com.tukan.api.service.mealplan.MealPlanEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanEngine mealPlanEngine;
    private final MealPlanAiService mealPlanAiService;

    @PostMapping("/ai/recommendations/meal-plan/me")
    public ResponseEntity<MealPlanRecomendacaoResponse> generateMealPlan(Authentication authentication) {
        MealPlanRecomendacaoResponse response = mealPlanAiService.generate(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/ai/recommendations/meal-plan/me/raw")
    public ResponseEntity<DailyMealPlan> generateRawMealPlan(Authentication authentication) {
        DailyMealPlan plan = mealPlanEngine.generatePlan(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }
}
