package com.tukan.api.service;

import com.tukan.api.dto.AdminUpdatePerfilRequest;
import com.tukan.api.dto.CreatePerfilRequest;
import com.tukan.api.dto.UpdatePerfilRequest;
import com.tukan.api.entity.Perfil;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.PerfilRepository;
import com.tukan.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final UserRepository userRepository;

    // ── Self-service ──────────────────────────────────────────────

    @Transactional
    public Perfil criarProprioPerfil(String emailAutenticado, CreatePerfilRequest request) {
        User usuario = buscarUsuarioPorEmail(emailAutenticado);

        if (perfilRepository.existsByUsuarioId(usuario.getId())) {
            throw new BusinessException("Este usuário já possui um perfil.", HttpStatus.CONFLICT);
        }

        return criarPerfil(usuario, request);
    }

    @Transactional(readOnly = true)
    public Perfil buscarProprioPerfil(String emailAutenticado) {
        User usuario = buscarUsuarioPorEmail(emailAutenticado);
        return buscarPerfilPorUsuarioId(usuario.getId());
    }

    @Transactional
    public Perfil atualizarProprioPerfil(String emailAutenticado, UpdatePerfilRequest request) {
        if (request.pesoKg() == null && request.alturaCm() == null && request.nivelAtividade() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        User usuario = buscarUsuarioPorEmail(emailAutenticado);
        Perfil perfil = buscarPerfilPorUsuarioId(usuario.getId());

        if (request.pesoKg() != null) {
            perfil.setPesoKg(request.pesoKg());
        }
        if (request.alturaCm() != null) {
            perfil.setAlturaCm(request.alturaCm());
        }
        if (request.nivelAtividade() != null) {
            perfil.setNivelAtividade(request.nivelAtividade());
        }

        return perfilRepository.save(perfil);
    }

    // ── Admin CRUD ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Perfil> listarTodos(Pageable pageable) {
        return perfilRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Perfil buscarPorId(Integer id) {
        return perfilRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Perfil criarPerfilParaUsuario(Integer usuarioId, CreatePerfilRequest request) {
        User usuario = buscarUsuarioPorId(usuarioId);

        if (perfilRepository.existsByUsuarioId(usuarioId)) {
            throw new BusinessException("Este usuário já possui um perfil.", HttpStatus.CONFLICT);
        }

        return criarPerfil(usuario, request);
    }

    @Transactional
    public Perfil atualizarPerfil(Integer id, AdminUpdatePerfilRequest request) {
        if (request.dataNascimento() == null && request.sexo() == null
                && request.pesoKg() == null && request.alturaCm() == null
                && request.nivelAtividade() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        Perfil perfil = buscarPorId(id);

        if (request.dataNascimento() != null) {
            perfil.setDataNascimento(request.dataNascimento());
        }
        if (request.sexo() != null) {
            perfil.setSexo(request.sexo());
        }
        if (request.pesoKg() != null) {
            perfil.setPesoKg(request.pesoKg());
        }
        if (request.alturaCm() != null) {
            perfil.setAlturaCm(request.alturaCm());
        }
        if (request.nivelAtividade() != null) {
            perfil.setNivelAtividade(request.nivelAtividade());
        }

        return perfilRepository.save(perfil);
    }

    @Transactional
    public void deletar(Integer id) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
        perfilRepository.delete(perfil);
    }

    // ── Métodos internos ──────────────────────────────────────────

    private Perfil criarPerfil(User usuario, CreatePerfilRequest request) {
        Perfil perfil = new Perfil();
        perfil.setUsuario(usuario);
        perfil.setDataNascimento(request.dataNascimento());
        perfil.setSexo(request.sexo());
        perfil.setPesoKg(request.pesoKg());
        perfil.setAlturaCm(request.alturaCm());
        perfil.setNivelAtividade(request.nivelAtividade());

        return perfilRepository.save(perfil);
    }

    private Perfil buscarPerfilPorUsuarioId(Integer usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
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
