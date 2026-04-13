package com.tukan.api.service;

import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.User;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserService userService;
    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final AssessmentRepository assessmentRepository;

    @Transactional(readOnly = true)
    public AuthenticatedUserData findAuthenticatedUserData(String email) {
        User user = userService.findByEmail(email);
        NutritionalProfile profile = nutritionalProfileRepository.findByUserId(user.getId()).orElse(null);
        Assessment assessment = assessmentRepository.findByUserId(user.getId()).orElse(null);
        return new AuthenticatedUserData(user, profile, assessment);
    }

    public record AuthenticatedUserData(User user, NutritionalProfile profile, Assessment assessment) {
    }
}