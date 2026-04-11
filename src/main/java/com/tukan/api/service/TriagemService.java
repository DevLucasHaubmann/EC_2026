package com.tukan.api.service;

import com.tukan.api.dto.AdminUpdateTriagemRequest;
import com.tukan.api.dto.CreateTriagemRequest;
import com.tukan.api.dto.UpdateTriagemRequest;
import com.tukan.api.entity.Triagem;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.TriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TriagemService {

    private final TriagemRepository triagemRepository;
    private final UserService userService;

    // ── Self-service ──────────────────────────────────────────────

    @Transactional
    public Triagem createOwn(String authenticatedEmail, CreateTriagemRequest request) {
        User usuario = userService.findByEmail(authenticatedEmail);

        if (triagemRepository.existsByUsuarioId(usuario.getId())) {
            throw new BusinessException("Este usuário já possui uma triagem.", HttpStatus.CONFLICT);
        }

        return createTriagem(usuario, request);
    }

    @Transactional(readOnly = true)
    public Triagem findOwn(String authenticatedEmail) {
        User usuario = userService.findByEmail(authenticatedEmail);
        return findByUsuarioId(usuario.getId());
    }

    @Transactional
    public Triagem updateOwn(String authenticatedEmail, UpdateTriagemRequest request) {
        if (request.objetivo() == null && request.restricoesAlimentares() == null
                && request.alergias() == null && request.condicoesSaude() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        User usuario = userService.findByEmail(authenticatedEmail);
        Triagem triagem = findByUsuarioId(usuario.getId());

        if (request.objetivo() != null) {
            triagem.setObjetivo(request.objetivo());
        }
        if (request.restricoesAlimentares() != null) {
            triagem.setRestricoesAlimentares(request.restricoesAlimentares());
        }
        if (request.alergias() != null) {
            triagem.setAlergias(request.alergias());
        }
        if (request.condicoesSaude() != null) {
            triagem.setCondicoesSaude(request.condicoesSaude());
        }

        return triagemRepository.save(triagem);
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Triagem> findAll(Pageable pageable) {
        return triagemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Triagem findById(Integer id) {
        return triagemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Triagem createForUser(Integer usuarioId, CreateTriagemRequest request) {
        User usuario = userService.findById(usuarioId);

        if (triagemRepository.existsByUsuarioId(usuarioId)) {
            throw new BusinessException("Este usuário já possui uma triagem.", HttpStatus.CONFLICT);
        }

        return createTriagem(usuario, request);
    }

    @Transactional
    public Triagem update(Integer id, AdminUpdateTriagemRequest request) {
        if (request.objetivo() == null && request.restricoesAlimentares() == null
                && request.alergias() == null && request.condicoesSaude() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        Triagem triagem = findById(id);

        if (request.objetivo() != null) {
            triagem.setObjetivo(request.objetivo());
        }
        if (request.restricoesAlimentares() != null) {
            triagem.setRestricoesAlimentares(request.restricoesAlimentares());
        }
        if (request.alergias() != null) {
            triagem.setAlergias(request.alergias());
        }
        if (request.condicoesSaude() != null) {
            triagem.setCondicoesSaude(request.condicoesSaude());
        }

        return triagemRepository.save(triagem);
    }

    @Transactional
    public void delete(Integer id) {
        Triagem triagem = triagemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
        triagemRepository.delete(triagem);
    }

    // ── Métodos internos ──────────────────────────────────────────

    private Triagem createTriagem(User usuario, CreateTriagemRequest request) {
        Triagem triagem = new Triagem();
        triagem.setUsuario(usuario);
        triagem.setObjetivo(request.objetivo());
        triagem.setRestricoesAlimentares(request.restricoesAlimentares());
        triagem.setAlergias(request.alergias());
        triagem.setCondicoesSaude(request.condicoesSaude());

        return triagemRepository.save(triagem);
    }

    private Triagem findByUsuarioId(Integer usuarioId) {
        return triagemRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
    }
}