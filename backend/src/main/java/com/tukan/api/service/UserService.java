package com.tukan.api.service;

import com.tukan.api.dto.UpdateUserRequest;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.util.EmailUtils;
import com.tukan.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionService userSessionService;
    private final UserDeletionService userDeletionService;

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Admin search by name or email. Expects a non-blank term (the controller
     * routes blank queries to {@link #findAll(Pageable)} to preserve the
     * existing unfiltered listing behavior).
     */
    public Page<User> search(String term, Pageable pageable) {
        return userRepository.searchByNameOrEmail(term.trim(), pageable);
    }

    public User findById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(EmailUtils.normalize(email))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public User update(Integer id, UpdateUserRequest request, String authenticatedEmail) {
        User authenticatedUser = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new BusinessException("Usuário autenticado não encontrado.", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));

        boolean isSelfUpdate = authenticatedUser.getId().equals(user.getId());

        if (isSelfUpdate) {
            rejectDestructiveSelfUpdate(request);
        }

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String normalizedEmail = EmailUtils.normalize(request.email());
            if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new BusinessException("E-mail já cadastrado.");
            }
            user.setEmail(normalizedEmail);
        }

        if (request.type() != null) {
            user.setType(request.type());
        }

        if (request.status() != null) {
            user.setStatus(request.status());
            if (request.status() != User.UserState.ACTIVE) {
                userSessionService.revokeAllSessions(id);
            }
        }

        return userRepository.save(user);
    }

    /**
     * Prevents an admin from demoting their own role or deactivating their own account.
     * Name and email changes on self are non-destructive and remain allowed.
     */
    private void rejectDestructiveSelfUpdate(UpdateUserRequest request) {
        if (request.status() != null && request.status() != User.UserState.ACTIVE) {
            throw new BusinessException(
                    "Você não pode executar esta ação contra a própria conta.", HttpStatus.FORBIDDEN);
        }
        if (request.type() != null && request.type() != User.UserType.ADMIN) {
            throw new BusinessException(
                    "Você não pode executar esta ação contra a própria conta.", HttpStatus.FORBIDDEN);
        }
    }

    @Transactional
    public void delete(Integer targetUserId, String authenticatedEmail) {
        User authenticatedUser = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new BusinessException("Usuário autenticado não encontrado.", HttpStatus.NOT_FOUND));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));

        boolean isSelfDeletion = authenticatedUser.getId().equals(targetUser.getId());

        if (isSelfDeletion) {
            throw new BusinessException("Você não pode executar esta ação contra a própria conta.", HttpStatus.FORBIDDEN);
        }

        userDeletionService.deleteUserAndOwnedData(targetUserId);
    }

    @Transactional
    public void revokeAllSessions(Integer id, String authenticatedEmail) {
        User authenticatedUser = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new BusinessException("Usuário autenticado não encontrado.", HttpStatus.NOT_FOUND));

        if (!userRepository.existsById(id)) {
            throw new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND);
        }

        if (authenticatedUser.getId().equals(id)) {
            throw new BusinessException(
                    "Você não pode executar esta ação contra a própria conta.", HttpStatus.FORBIDDEN);
        }

        userSessionService.revokeAllSessions(id);
    }

}
