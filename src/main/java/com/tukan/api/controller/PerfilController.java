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
    public ResponseEntity<PerfilCreatedResponse> criarProprioPerfil(
            @RequestBody @Valid CreatePerfilRequest request,
            Authentication authentication) {
        Perfil perfil = perfilService.criarProprioPerfil(authentication.getName(), request);
        OnboardingStatus onboarding = onboardingService.verificarOnboarding(perfil.getUsuario().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PerfilCreatedResponse.of(perfil, onboarding));
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> buscarProprioPerfil(Authentication authentication) {
        return ResponseEntity.ok(
                PerfilResponse.from(perfilService.buscarProprioPerfil(authentication.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<PerfilResponse> atualizarProprioPerfil(
            @RequestBody @Valid UpdatePerfilRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                PerfilResponse.from(
                        perfilService.atualizarProprioPerfil(authentication.getName(), request)));
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<PerfilResponse>> listarTodos(Pageable pageable) {
        Page<PerfilResponse> perfis = perfilService.listarTodos(pageable)
                .map(PerfilResponse::from);
        return ResponseEntity.ok(perfis);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PerfilResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(PerfilResponse.from(perfilService.buscarPorId(id)));
    }

    @PostMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PerfilResponse> criarPerfilParaUsuario(
            @PathVariable Integer usuarioId,
            @RequestBody @Valid CreatePerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PerfilResponse.from(
                        perfilService.criarPerfilParaUsuario(usuarioId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PerfilResponse> atualizarPerfil(
            @PathVariable Integer id,
            @RequestBody @Valid AdminUpdatePerfilRequest request) {
        return ResponseEntity.ok(
                PerfilResponse.from(perfilService.atualizarPerfil(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarPerfil(@PathVariable Integer id) {
        perfilService.deletar(id);
    }
}
