package com.tukan.api.service;

import com.tukan.api.dto.AdminUpdateProfileRequest;
import com.tukan.api.dto.CreateProfileRequest;
import com.tukan.api.dto.UpdateProfileRequest;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.NutritionalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NutritionalProfileService {

    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final UserService userService;

    // ── Self-service ──────────────────────────────────────────────

    @Transactional
    public NutritionalProfile createOwn(String authenticatedEmail, CreateProfileRequest request) {
        User user = userService.findByEmail(authenticatedEmail);

        if (nutritionalProfileRepository.existsByUserId(user.getId())) {
            throw new BusinessException("Este usuário já possui um perfil.", HttpStatus.CONFLICT);
        }

        return createProfile(user, request);
    }

    @Transactional(readOnly = true)
    public NutritionalProfile findOwn(String authenticatedEmail) {
        User user = userService.findByEmail(authenticatedEmail);
        return findByUserId(user.getId());
    }

    @Transactional
    public NutritionalProfile updateOwn(String authenticatedEmail, UpdateProfileRequest request) {
        if (request.weightKg() == null && request.heightCm() == null && request.activityLevel() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        User user = userService.findByEmail(authenticatedEmail);
        NutritionalProfile profile = findByUserId(user.getId());

        if (request.weightKg() != null) {
            profile.setWeightKg(request.weightKg());
        }
        if (request.heightCm() != null) {
            profile.setHeightCm(request.heightCm());
        }
        if (request.activityLevel() != null) {
            profile.setActivityLevel(request.activityLevel());
        }

        return nutritionalProfileRepository.save(profile);
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NutritionalProfile> findAll(Pageable pageable) {
        return nutritionalProfileRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public NutritionalProfile findById(Integer id) {
        return nutritionalProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public NutritionalProfile createForUser(Integer userId, CreateProfileRequest request) {
        User user = userService.findById(userId);

        if (nutritionalProfileRepository.existsByUserId(userId)) {
            throw new BusinessException("Este usuário já possui um perfil.", HttpStatus.CONFLICT);
        }

        return createProfile(user, request);
    }

    @Transactional
    public NutritionalProfile update(Integer id, AdminUpdateProfileRequest request) {
        if (request.dateOfBirth() == null && request.gender() == null
                && request.weightKg() == null && request.heightCm() == null
                && request.activityLevel() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        NutritionalProfile profile = findById(id);

        if (request.dateOfBirth() != null) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }
        if (request.weightKg() != null) {
            profile.setWeightKg(request.weightKg());
        }
        if (request.heightCm() != null) {
            profile.setHeightCm(request.heightCm());
        }
        if (request.activityLevel() != null) {
            profile.setActivityLevel(request.activityLevel());
        }

        return nutritionalProfileRepository.save(profile);
    }

    @Transactional
    public void delete(Integer id) {
        NutritionalProfile profile = nutritionalProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
        nutritionalProfileRepository.delete(profile);
    }

    // ── Internal methods ──────────────────────────────────────────

    private NutritionalProfile createProfile(User user, CreateProfileRequest request) {
        NutritionalProfile profile = new NutritionalProfile();
        profile.setUser(user);
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setWeightKg(request.weightKg());
        profile.setHeightCm(request.heightCm());
        profile.setActivityLevel(request.activityLevel());

        return nutritionalProfileRepository.save(profile);
    }

    public NutritionalProfile findByUserId(Integer userId) {
        return nutritionalProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
    }
}
