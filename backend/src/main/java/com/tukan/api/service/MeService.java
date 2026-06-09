package com.tukan.api.service;

import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserService userService;
    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final AssessmentRepository assessmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AuthenticatedUserData findAuthenticatedUserData(String email) {
        User user = userService.findByEmail(email);
        NutritionalProfile profile = nutritionalProfileRepository.findByUserId(user.getId()).orElse(null);
        Assessment assessment = assessmentRepository.findByUserId(user.getId()).orElse(null);
        return new AuthenticatedUserData(user, profile, assessment);
    }

    @Transactional
    public AuthenticatedUserData updateAvatar(String email, String avatarUrl) {
        User user = userService.findByEmail(email);

        String sanitized = (avatarUrl == null || avatarUrl.isBlank()) ? null : avatarUrl.trim();

        if (sanitized != null && !isSafeAvatarUrl(sanitized)) {
            throw new BusinessException(
                    "URL do avatar inválida. Informe uma URL com protocolo http:// ou https://.",
                    HttpStatus.BAD_REQUEST
            );
        }

        user.setAvatarUrl(sanitized);
        userRepository.save(user);

        NutritionalProfile profile = nutritionalProfileRepository.findByUserId(user.getId()).orElse(null);
        Assessment assessment = assessmentRepository.findByUserId(user.getId()).orElse(null);
        return new AuthenticatedUserData(user, profile, assessment);
    }

    /**
     * Accepts only http:// and https:// URLs with a non-empty host.
     * Rejects javascript:, data:, file:, and bare URLs without a protocol.
     *
     * Uses java.net.URI for scheme extraction to avoid fragile regex.
     * Note: http:// is accepted to allow local development environments;
     * production deployments should enforce https:// at the infrastructure level.
     */
    private boolean isSafeAvatarUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            String schemeLower = scheme.toLowerCase();
            if (!schemeLower.equals("http") && !schemeLower.equals("https")) {
                return false;
            }
            String host = uri.getHost();
            return host != null && !host.isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public record AuthenticatedUserData(User user, NutritionalProfile profile, Assessment assessment) {
    }
}