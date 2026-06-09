package com.tukan.api.service;

import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.BodyWeightLogRepository;
import com.tukan.api.repository.GenerationJobRepository;
import com.tukan.api.repository.MealLogRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.repository.RecommendationFeedbackRepository;
import com.tukan.api.repository.RecommendationRepository;
import com.tukan.api.repository.UserActiveDayRepository;
import com.tukan.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsible for removing a user and all user-owned data in the correct FK order.
 * Deletion order (children before parents, respecting FK constraints):
 *   1. recommendation_feedback  (child of recommendation)
 *   2. meal_log                 (child of user + recommendation)
 *   3. generation_job           (child of user)
 *   4. recommendation           (child of user — after feedback and meal_log)
 *   5. assessment               (child of user)
 *   6. nutritional_profile      (child of user)
 *   7. user_active_day          (child of user)
 *   8. body_weight_log          (child of user)
 *   9. user_session             (child of user)
 *  10. app_user                 (root — deleted last)
 *
 * All steps run in the same transaction: any failure rolls back the entire cascade.
 */
@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final MealLogRepository mealLogRepository;
    private final GenerationJobRepository generationJobRepository;
    private final RecommendationRepository recommendationRepository;
    private final AssessmentRepository assessmentRepository;
    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final UserActiveDayRepository userActiveDayRepository;
    private final BodyWeightLogRepository bodyWeightLogRepository;
    private final UserSessionService userSessionService;
    private final UserRepository userRepository;

    @Transactional
    public void deleteUserAndOwnedData(Integer userId) {
        recommendationFeedbackRepository.deleteByRecommendationUserId(userId);
        mealLogRepository.deleteByUserId(userId);
        generationJobRepository.deleteByUserId(userId);
        recommendationRepository.deleteByUserId(userId);
        assessmentRepository.deleteByUserId(userId);
        nutritionalProfileRepository.deleteByUserId(userId);
        userActiveDayRepository.deleteByUserId(userId);
        bodyWeightLogRepository.deleteByUserId(userId);
        userSessionService.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
