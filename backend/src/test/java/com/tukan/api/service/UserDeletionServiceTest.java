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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UserDeletionServiceTest {

    @Mock
    private RecommendationFeedbackRepository recommendationFeedbackRepository;

    @Mock
    private MealLogRepository mealLogRepository;

    @Mock
    private GenerationJobRepository generationJobRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private NutritionalProfileRepository nutritionalProfileRepository;

    @Mock
    private UserActiveDayRepository userActiveDayRepository;

    @Mock
    private BodyWeightLogRepository bodyWeightLogRepository;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDeletionService userDeletionService;

    @Test
    void shouldDeleteAllUserOwnedDataInCorrectOrder() {
        // given
        Integer userId = 42;

        // when
        userDeletionService.deleteUserAndOwnedData(userId);

        // then — FK order: children before parents
        InOrder order = inOrder(
                recommendationFeedbackRepository,
                mealLogRepository,
                generationJobRepository,
                recommendationRepository,
                assessmentRepository,
                nutritionalProfileRepository,
                userActiveDayRepository,
                bodyWeightLogRepository,
                userSessionService,
                userRepository
        );
        order.verify(recommendationFeedbackRepository).deleteByRecommendationUserId(userId);
        order.verify(mealLogRepository).deleteByUserId(userId);
        order.verify(generationJobRepository).deleteByUserId(userId);
        order.verify(recommendationRepository).deleteByUserId(userId);
        order.verify(assessmentRepository).deleteByUserId(userId);
        order.verify(nutritionalProfileRepository).deleteByUserId(userId);
        order.verify(userActiveDayRepository).deleteByUserId(userId);
        order.verify(bodyWeightLogRepository).deleteByUserId(userId);
        order.verify(userSessionService).deleteAllByUserId(userId);
        order.verify(userRepository).deleteById(userId);
    }
}
