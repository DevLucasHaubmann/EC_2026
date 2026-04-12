package com.tukan.api.service;

import com.tukan.api.dto.UpdateUserRequest;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.util.EmailUtils;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionService userSessionService;
    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final AssessmentRepository assessmentRepository;

    public List<User> findAll() {
        return userRepository.findAll();
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
    public User update(Integer id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));

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

    @Transactional
    public void delete(Integer targetUserId, String authenticatedEmail) {
        User authenticatedUser = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new BusinessException("Usuário autenticado não encontrado."));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        boolean isSelfDeletion = authenticatedUser.getId().equals(targetUser.getId());

        if (isSelfDeletion) {
            throw new BusinessException("Um administrador não pode excluir a própria conta.", HttpStatus.FORBIDDEN);
        }

        assessmentRepository.deleteByUserId(targetUserId);
        nutritionalProfileRepository.deleteByUserId(targetUserId);
        userSessionService.deleteAllByUserId(targetUserId);
        userRepository.delete(targetUser);
    }

    @Transactional
    public void revokeAllSessions(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND);
        }
        userSessionService.revokeAllSessions(id);
    }

}
