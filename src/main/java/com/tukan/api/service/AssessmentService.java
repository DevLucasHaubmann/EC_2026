package com.tukan.api.service;

import com.tukan.api.dto.AdminUpdateTriagemRequest;
import com.tukan.api.dto.CreateTriagemRequest;
import com.tukan.api.dto.UpdateTriagemRequest;
import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final UserService userService;

    // ── Self-service ──────────────────────────────────────────────

    @Transactional
    public Assessment createOwn(String authenticatedEmail, CreateTriagemRequest request) {
        User user = userService.findByEmail(authenticatedEmail);

        if (assessmentRepository.existsByUserId(user.getId())) {
            throw new BusinessException("Este usuário já possui uma triagem.", HttpStatus.CONFLICT);
        }

        return createAssessment(user, request);
    }

    @Transactional(readOnly = true)
    public Assessment findOwn(String authenticatedEmail) {
        User user = userService.findByEmail(authenticatedEmail);
        return findByUserId(user.getId());
    }

    @Transactional
    public Assessment updateOwn(String authenticatedEmail, UpdateTriagemRequest request) {
        if (request.goal() == null && request.dietaryRestrictions() == null
                && request.allergies() == null && request.healthConditions() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        User user = userService.findByEmail(authenticatedEmail);
        Assessment assessment = findByUserId(user.getId());

        if (request.goal() != null) {
            assessment.setGoal(request.goal());
        }
        if (request.dietaryRestrictions() != null) {
            assessment.setDietaryRestrictions(request.dietaryRestrictions());
        }
        if (request.allergies() != null) {
            assessment.setAllergies(request.allergies());
        }
        if (request.healthConditions() != null) {
            assessment.setHealthConditions(request.healthConditions());
        }

        return assessmentRepository.save(assessment);
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Assessment> findAll(Pageable pageable) {
        return assessmentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Assessment findById(Integer id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Assessment createForUser(Integer userId, CreateTriagemRequest request) {
        User user = userService.findById(userId);

        if (assessmentRepository.existsByUserId(userId)) {
            throw new BusinessException("Este usuário já possui uma triagem.", HttpStatus.CONFLICT);
        }

        return createAssessment(user, request);
    }

    @Transactional
    public Assessment update(Integer id, AdminUpdateTriagemRequest request) {
        if (request.goal() == null && request.dietaryRestrictions() == null
                && request.allergies() == null && request.healthConditions() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        Assessment assessment = findById(id);

        if (request.goal() != null) {
            assessment.setGoal(request.goal());
        }
        if (request.dietaryRestrictions() != null) {
            assessment.setDietaryRestrictions(request.dietaryRestrictions());
        }
        if (request.allergies() != null) {
            assessment.setAllergies(request.allergies());
        }
        if (request.healthConditions() != null) {
            assessment.setHealthConditions(request.healthConditions());
        }

        return assessmentRepository.save(assessment);
    }

    @Transactional
    public void delete(Integer id) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
        assessmentRepository.delete(assessment);
    }

    // ── Internal methods ──────────────────────────────────────────

    private Assessment createAssessment(User user, CreateTriagemRequest request) {
        Assessment assessment = new Assessment();
        assessment.setUser(user);
        assessment.setGoal(request.goal());
        assessment.setDietaryRestrictions(request.dietaryRestrictions());
        assessment.setAllergies(request.allergies());
        assessment.setHealthConditions(request.healthConditions());

        return assessmentRepository.save(assessment);
    }

    private Assessment findByUserId(Integer userId) {
        return assessmentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
    }
}
