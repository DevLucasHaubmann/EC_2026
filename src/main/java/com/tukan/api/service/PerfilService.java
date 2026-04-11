package com.tukan.api.service;

import com.tukan.api.dto.AdminUpdatePerfilRequest;
import com.tukan.api.dto.CreatePerfilRequest;
import com.tukan.api.dto.UpdatePerfilRequest;
import com.tukan.api.entity.Perfil;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.PerfilRepository;
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
    private final UserService userService;

    // ── Self-service ──────────────────────────────────────────────

    @Transactional
    public Perfil createOwn(String authenticatedEmail, CreatePerfilRequest request) {
        User usuario = userService.findByEmail(authenticatedEmail);

        if (perfilRepository.existsByUsuarioId(usuario.getId())) {
            throw new BusinessException("Este usuário já possui um perfil.", HttpStatus.CONFLICT);
        }

        return createPerfil(usuario, request);
    }

    @Transactional(readOnly = true)
    public Perfil findOwn(String authenticatedEmail) {
        User usuario = userService.findByEmail(authenticatedEmail);
        return findByUsuarioId(usuario.getId());
    }

    @Transactional
    public Perfil updateOwn(String authenticatedEmail, UpdatePerfilRequest request) {
        if (request.pesoKg() == null && request.alturaCm() == null && request.nivelAtividade() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        User usuario = userService.findByEmail(authenticatedEmail);
        Perfil perfil = findByUsuarioId(usuario.getId());

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
    public Page<Perfil> findAll(Pageable pageable) {
        return perfilRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Perfil findById(Integer id) {
        return perfilRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Perfil createForUser(Integer usuarioId, CreatePerfilRequest request) {
        User usuario = userService.findById(usuarioId);

        if (perfilRepository.existsByUsuarioId(usuarioId)) {
            throw new BusinessException("Este usuário já possui um perfil.", HttpStatus.CONFLICT);
        }

        return createPerfil(usuario, request);
    }

    @Transactional
    public Perfil update(Integer id, AdminUpdatePerfilRequest request) {
        if (request.dataNascimento() == null && request.sexo() == null
                && request.pesoKg() == null && request.alturaCm() == null
                && request.nivelAtividade() == null) {
            throw new BusinessException("Informe pelo menos um campo para atualizar.", HttpStatus.BAD_REQUEST);
        }

        Perfil perfil = findById(id);

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
    public void delete(Integer id) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
        perfilRepository.delete(perfil);
    }

    // ── Métodos internos ──────────────────────────────────────────

    private Perfil createPerfil(User usuario, CreatePerfilRequest request) {
        Perfil perfil = new Perfil();
        perfil.setUsuario(usuario);
        perfil.setDataNascimento(request.dataNascimento());
        perfil.setSexo(request.sexo());
        perfil.setPesoKg(request.pesoKg());
        perfil.setAlturaCm(request.alturaCm());
        perfil.setNivelAtividade(request.nivelAtividade());

        return perfilRepository.save(perfil);
    }

    private Perfil findByUsuarioId(Integer usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Perfil não encontrado.", HttpStatus.NOT_FOUND));
    }
}