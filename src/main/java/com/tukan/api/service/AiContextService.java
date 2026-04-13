package com.tukan.api.service;

import com.tukan.api.dto.ai.AiPerfilContext;
import com.tukan.api.dto.ai.AiRecommendationContext;
import com.tukan.api.dto.ai.AiTriagemContext;
import com.tukan.api.dto.ai.AiUsuarioContext;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.User;
import com.tukan.api.exception.IncompleteProfileException;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiContextService {

    private final UserService userService;
    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final AssessmentRepository assessmentRepository;

    @Transactional(readOnly = true)
    public AiRecommendationContext build(Integer userId) {
        User user = userService.findById(userId);

        NutritionalProfile profile = nutritionalProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IncompleteProfileException(
                        "perfil",
                        "Perfil nutricional não encontrado. Complete seu perfil antes de solicitar recomendações."));

        Assessment assessment = assessmentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IncompleteProfileException(
                        "triagem",
                        "Triagem não encontrada. Complete sua triagem antes de solicitar recomendações."));

        return new AiRecommendationContext(
                buildUserContext(user),
                buildProfileContext(profile),
                buildAssessmentContext(assessment)
        );
    }

    private AiUsuarioContext buildUserContext(User user) {
        return new AiUsuarioContext(user.getName());
    }

    private AiPerfilContext buildProfileContext(NutritionalProfile profile) {
        return new AiPerfilContext(
                profile.getGender().name(),
                profile.calculateAge(LocalDate.now()),
                profile.getWeightKg(),
                profile.getHeightCm(),
                profile.getActivityLevel().name()
        );
    }

    private AiTriagemContext buildAssessmentContext(Assessment assessment) {
        return new AiTriagemContext(
                assessment.getGoal().name(),
                normalizeList(assessment.getDietaryRestrictions()),
                normalizeList(assessment.getAllergies()),
                normalizeList(assessment.getHealthConditions())
        );
    }

    private List<String> normalizeList(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split("[,;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
