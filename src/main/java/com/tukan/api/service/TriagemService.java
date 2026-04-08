package com.tukan.api.service;

import com.tukan.api.dto.AdminUpdateTriagemRequest;
import com.tukan.api.dto.CreateTriagemRequest;
import com.tukan.api.dto.UpdateTriagemRequest;
import com.tukan.api.entity.Triagem;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.TriagemRepository;
import com.tukan.api.repository.UserRepository;
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
    private final UserRepository userRepository;

    // ── Self-service ──────────────────────────────────────────────

    @Transactional
    public Triagem criarPropriaTriagem(String emailAutenticado, CreateTriagemRequest request) {
        User usuario = buscarUsuarioPorEmail(emailAutenticado);

        if (triagemRepository.existsByUsuarioId(usuario.getId())) {
            throw new BusinessException("Este usuário já possui uma triagem.", HttpStatus.CONFLICT);
        }

        return criarTriagem(usuario, request);
    }

    @Transactional(readOnly = true)
    public Triagem buscarPropriaTriagem(String emailAutenticado) {
        User usuario = buscarUsuarioPorEmail(emailAutenticado);
        return buscarTriagemPorUsuarioId(usuario.getId());
    }

    @Transactional
    public Triagem atualizarPropriaTriagem(String emailAutenticado, UpdateTriagemRequest request) {
        User usuario = buscarUsuarioPorEmail(emailAutenticado);
        Triagem triagem = buscarTriagemPorUsuarioId(usuario.getId());

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
    public Page<Triagem> listarTodas(Pageable pageable) {
        return triagemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Triagem buscarPorId(Integer id) {
        return triagemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Triagem criarTriagemParaUsuario(Integer usuarioId, CreateTriagemRequest request) {
        User usuario = buscarUsuarioPorId(usuarioId);

        if (triagemRepository.existsByUsuarioId(usuarioId)) {
            throw new BusinessException("Este usuário já possui uma triagem.", HttpStatus.CONFLICT);
        }

        return criarTriagem(usuario, request);
    }

    @Transactional
    public Triagem atualizarTriagem(Integer id, AdminUpdateTriagemRequest request) {
        Triagem triagem = buscarPorId(id);

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
    public void deletar(Integer id) {
        Triagem triagem = triagemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
        triagemRepository.delete(triagem);
    }

    // ── Métodos internos ──────────────────────────────────────────

    private Triagem criarTriagem(User usuario, CreateTriagemRequest request) {
        Triagem triagem = new Triagem();
        triagem.setUsuario(usuario);
        triagem.setObjetivo(request.objetivo());
        triagem.setRestricoesAlimentares(request.restricoesAlimentares());
        triagem.setAlergias(request.alergias());
        triagem.setCondicoesSaude(request.condicoesSaude());

        return triagemRepository.save(triagem);
    }

    private Triagem buscarTriagemPorUsuarioId(Integer usuarioId) {
        return triagemRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Triagem não encontrada.", HttpStatus.NOT_FOUND));
    }

    private User buscarUsuarioPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));
    }

    private User buscarUsuarioPorId(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));
    }
}
