package com.tukan.api.controller;

import com.tukan.api.dto.AdminUpdateTriagemRequest;
import com.tukan.api.dto.CreateTriagemRequest;
import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.dto.TriagemCreatedResponse;
import com.tukan.api.dto.TriagemResponse;
import com.tukan.api.dto.UpdateTriagemRequest;
import com.tukan.api.entity.Triagem;
import com.tukan.api.service.OnboardingService;
import com.tukan.api.service.TriagemService;
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
@RequestMapping("/triagem")
@RequiredArgsConstructor
public class TriagemController {

    private final TriagemService triagemService;
    private final OnboardingService onboardingService;

    // ── Self-service (qualquer usuário autenticado) ───────────────

    @PostMapping("/me")
    public ResponseEntity<TriagemCreatedResponse> criarPropriaTriagem(
            @RequestBody @Valid CreateTriagemRequest request,
            Authentication authentication) {
        Triagem triagem = triagemService.criarPropriaTriagem(authentication.getName(), request);
        OnboardingStatus onboarding = onboardingService.verificarOnboarding(triagem.getUsuario().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TriagemCreatedResponse.of(triagem, onboarding));
    }

    @GetMapping("/me")
    public ResponseEntity<TriagemResponse> buscarPropriaTriagem(Authentication authentication) {
        return ResponseEntity.ok(
                TriagemResponse.from(triagemService.buscarPropriaTriagem(authentication.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<TriagemResponse> atualizarPropriaTriagem(
            @RequestBody @Valid UpdateTriagemRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                TriagemResponse.from(
                        triagemService.atualizarPropriaTriagem(authentication.getName(), request)));
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<TriagemResponse>> listarTodas(Pageable pageable) {
        Page<TriagemResponse> triagens = triagemService.listarTodas(pageable)
                .map(TriagemResponse::from);
        return ResponseEntity.ok(triagens);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TriagemResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(TriagemResponse.from(triagemService.buscarPorId(id)));
    }

    @PostMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TriagemResponse> criarTriagemParaUsuario(
            @PathVariable Integer usuarioId,
            @RequestBody @Valid CreateTriagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TriagemResponse.from(
                        triagemService.criarTriagemParaUsuario(usuarioId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TriagemResponse> atualizarTriagem(
            @PathVariable Integer id,
            @RequestBody @Valid AdminUpdateTriagemRequest request) {
        return ResponseEntity.ok(
                TriagemResponse.from(triagemService.atualizarTriagem(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarTriagem(@PathVariable Integer id) {
        triagemService.deletar(id);
    }
}
