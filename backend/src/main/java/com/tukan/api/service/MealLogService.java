package com.tukan.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.meallog.LogMealRequest;
import com.tukan.api.dto.meallog.MealLogResponse;
import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealOption;
import com.tukan.api.dto.mealplan.MealPlanMeal;
import com.tukan.api.entity.MealLog;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.MealLogRepository;
import com.tukan.api.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MealLogResponse logMeal(String email, LogMealRequest request) {
        User user = findUserByEmail(email);
        Recommendation recommendation = findActiveRecommendationForUser(request.recommendationId(), user.getId());

        // Duplicate check before plan parsing intentionally: avoids JSON deserialization on concurrent retries.
        validateDuplicate(user.getId(), request.mealDate(), request.mealType());

        MealOption option = extractMealOption(
                parsePlanJson(recommendation.getPlanJson()),
                request.mealType(),
                request.optionNumber()
        );

        MealLog log = buildMealLog(user, recommendation.getId(), request, option);
        return MealLogResponse.from(mealLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<MealLogResponse> getLogsByDate(String email, LocalDate date) {
        User user = findUserByEmail(email);
        return mealLogRepository.findByUserIdAndMealDate(user.getId(), date)
                .stream()
                .map(MealLogResponse::from)
                .toList();
    }

    @Transactional
    public void deleteLog(String email, Long logId) {
        User user = findUserByEmail(email);
        MealLog log = mealLogRepository.findByIdAndUserId(logId, user.getId())
                .orElseThrow(() -> new BusinessException(
                        "Registro de refeição não encontrado.", HttpStatus.NOT_FOUND));
        mealLogRepository.delete(log);
    }

    // --- private helpers ---

    private User findUserByEmail(String email) {
        return userService.findByEmail(email);
    }

    private Recommendation findActiveRecommendationForUser(Integer recommendationId, Integer userId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(
                        "Recomendação não encontrada.", HttpStatus.NOT_FOUND));

        if (!recommendation.getUser().getId().equals(userId)) {
            throw new BusinessException(
                    "Acesso negado a esta recomendação.", HttpStatus.FORBIDDEN);
        }

        if (!Recommendation.ACTIVE_STATUSES.contains(recommendation.getStatus())) {
            throw new BusinessException(
                    "Somente recomendações ativas (GENERATED ou VIEWED) podem ser usadas para registrar refeições.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return recommendation;
    }

    private void validateDuplicate(Integer userId, LocalDate mealDate, String mealType) {
        if (mealLogRepository.existsByUserIdAndMealDateAndMealType(userId, mealDate, mealType)) {
            throw new BusinessException(
                    "Já existe um registro para esta refeição nesta data.", HttpStatus.CONFLICT);
        }
    }

    private DailyMealPlan parsePlanJson(String planJson) {
        try {
            return objectMapper.readValue(planJson, new TypeReference<DailyMealPlan>() {});
        } catch (Exception e) {
            throw new BusinessException(
                    "Plano alimentar em formato inválido. Contate o suporte.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private MealOption extractMealOption(DailyMealPlan plan, String mealType, int optionNumber) {
        MealPlanMeal meal = plan.meals().stream()
                .filter(m -> mealType.equals(m.mealType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Tipo de refeição '" + mealType + "' não encontrado no plano alimentar.",
                        HttpStatus.UNPROCESSABLE_ENTITY));

        return meal.options().stream()
                .filter(o -> o.optionNumber() == optionNumber)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Opção " + optionNumber + " não encontrada para a refeição '" + mealType + "'.",
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private MealLog buildMealLog(User user, Integer recommendationId, LogMealRequest request, MealOption option) {
        MealLog log = new MealLog();
        log.setUser(user);
        log.setRecommendationId(recommendationId);
        log.setMealDate(request.mealDate());
        log.setMealType(request.mealType());
        log.setOptionNumber(request.optionNumber());
        log.setCalories(option.totalCalories());
        log.setProtein(option.totalProtein());
        log.setCarbs(option.totalCarbs());
        log.setFat(option.totalFat());
        log.setNotes(request.notes());
        return log;
    }
}
