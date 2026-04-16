package com.tukan.api.controller;

import com.tukan.api.dto.AdminUpdateProfileRequest;
import com.tukan.api.dto.CreateProfileRequest;
import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.dto.ProfileCreatedResponse;
import com.tukan.api.dto.ProfileResponse;
import com.tukan.api.dto.UpdateProfileRequest;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.service.OnboardingService;
import com.tukan.api.service.NutritionalProfileService;
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
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final NutritionalProfileService nutritionalProfileService;
    private final OnboardingService onboardingService;

    // ── Self-service (qualquer usuário autenticado) ───────────────

    @PostMapping("/me")
    public ResponseEntity<ProfileCreatedResponse> createOwn(
            @RequestBody @Valid CreateProfileRequest request,
            Authentication authentication) {
        NutritionalProfile profile = nutritionalProfileService.createOwn(authentication.getName(), request);
        OnboardingStatus onboarding = onboardingService.checkOnboarding(profile.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProfileCreatedResponse.of(profile, onboarding));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> findOwn(Authentication authentication) {
        return ResponseEntity.ok(
                ProfileResponse.from(nutritionalProfileService.findOwn(authentication.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateOwn(
            @RequestBody @Valid UpdateProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                ProfileResponse.from(
                        nutritionalProfileService.updateOwn(authentication.getName(), request)));
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ProfileResponse>> findAll(Pageable pageable) {
        Page<ProfileResponse> profiles = nutritionalProfileService.findAll(pageable)
                .map(ProfileResponse::from);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ProfileResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(ProfileResponse.from(nutritionalProfileService.findById(id)));
    }

    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ProfileResponse> createForUser(
            @PathVariable Integer userId,
            @RequestBody @Valid CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProfileResponse.from(
                        nutritionalProfileService.createForUser(userId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ProfileResponse> update(
            @PathVariable Integer id,
            @RequestBody @Valid AdminUpdateProfileRequest request) {
        return ResponseEntity.ok(
                ProfileResponse.from(nutritionalProfileService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        nutritionalProfileService.delete(id);
    }
}