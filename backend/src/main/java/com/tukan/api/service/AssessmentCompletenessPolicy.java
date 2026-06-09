package com.tukan.api.service;

import com.tukan.api.entity.Assessment;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for what constitutes a complete assessment.
 * An assessment is complete when the three fields required for meal plan
 * generation are all non-null: goal, dietType, and mealsPerDay.
 */
@Component
public class AssessmentCompletenessPolicy {

    public boolean isComplete(Assessment assessment) {
        if (assessment == null) return false;
        if (assessment.getGoal() == null) return false;
        if (assessment.getDietType() == null) return false;
        return assessment.getMealsPerDay() != null;
    }
}
