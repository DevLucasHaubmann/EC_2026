package com.tukan.api.controller;

import com.tukan.api.dto.AdminUpdateAssessmentRequest;
import com.tukan.api.dto.CreateAssessmentRequest;
import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.dto.AssessmentCreatedResponse;
import com.tukan.api.dto.AssessmentResponse;
import com.tukan.api.dto.UpdateAssessmentRequest;
import com.tukan.api.entity.Assessment;
import com.tukan.api.service.OnboardingService;
import com.tukan.api.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final OnboardingService onboardingService;

    // ── Self-service (qualquer usuário autenticado) ───────────────

    @PostMapping("/me")
    public ResponseEntity<AssessmentCreatedResponse> createOwn(
            @RequestBody @Valid CreateAssessmentRequest request,
            Authentication authentication) {
        Assessment assessment = assessmentService.createOwn(authentication.getName(), request);
        OnboardingStatus onboarding = onboardingService.checkOnboarding(assessment.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AssessmentCreatedResponse.of(assessment, onboarding));
    }

    @GetMapping("/me")
    public ResponseEntity<AssessmentResponse> findOwn(Authentication authentication) {
        return ResponseEntity.ok(
                AssessmentResponse.from(assessmentService.findOwn(authentication.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<AssessmentResponse> updateOwn(
            @RequestBody @Valid UpdateAssessmentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                AssessmentResponse.from(
                        assessmentService.updateOwn(authentication.getName(), request)));
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<AssessmentResponse>> findAll(Pageable pageable) {
        Page<AssessmentResponse> assessments = assessmentService.findAll(pageable)
                .map(AssessmentResponse::from);
        return ResponseEntity.ok(assessments);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AssessmentResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(AssessmentResponse.from(assessmentService.findById(id)));
    }

    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AssessmentResponse> createForUser(
            @PathVariable Integer userId,
            @RequestBody @Valid CreateAssessmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AssessmentResponse.from(
                        assessmentService.createForUser(userId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AssessmentResponse> update(
            @PathVariable Integer id,
            @RequestBody @Valid AdminUpdateAssessmentRequest request) {
        return ResponseEntity.ok(
                AssessmentResponse.from(assessmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        assessmentService.delete(id);
    }
}