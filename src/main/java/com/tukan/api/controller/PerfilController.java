package com.tukan.api.controller;

import com.tukan.api.dto.AdminUpdatePerfilRequest;
import com.tukan.api.dto.CreatePerfilRequest;
import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.dto.PerfilCreatedResponse;
import com.tukan.api.dto.PerfilResponse;
import com.tukan.api.dto.UpdatePerfilRequest;
import com.tukan.api.entity.Perfil;
import com.tukan.api.service.OnboardingService;
import com.tukan.api.service.PerfilService;
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
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;
    private final OnboardingService onboardingService;

    // ── Self-service (qualquer usuário autenticado) ───────────────

    @PostMapping("/me")
    public ResponseEntity<PerfilCreatedResponse> createOwn(
            @RequestBody @Valid CreatePerfilRequest request,
            Authentication authentication) {
        Perfil perfil = perfilService.createOwn(authentication.getName(), request);
        OnboardingStatus onboarding = onboardingService.checkOnboarding(perfil.getUsuario().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PerfilCreatedResponse.of(perfil, onboarding));
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> findOwn(Authentication authentication) {
        return ResponseEntity.ok(
                PerfilResponse.from(perfilService.findOwn(authentication.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<PerfilResponse> updateOwn(
            @RequestBody @Valid UpdatePerfilRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                PerfilResponse.from(
                        perfilService.updateOwn(authentication.getName(), request)));
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<PerfilResponse>> findAll(Pageable pageable) {
        Page<PerfilResponse> perfis = perfilService.findAll(pageable)
                .map(PerfilResponse::from);
        return ResponseEntity.ok(perfis);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PerfilResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(PerfilResponse.from(perfilService.findById(id)));
    }

    @PostMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PerfilResponse> createForUser(
            @PathVariable Integer usuarioId,
            @RequestBody @Valid CreatePerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PerfilResponse.from(
                        perfilService.createForUser(usuarioId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PerfilResponse> update(
            @PathVariable Integer id,
            @RequestBody @Valid AdminUpdatePerfilRequest request) {
        return ResponseEntity.ok(
                PerfilResponse.from(perfilService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        perfilService.delete(id);
    }
}