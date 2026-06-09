package com.tukan.api.service;

import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceDeleteTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private UserDeletionService userDeletionService;

    @InjectMocks
    private UserService userService;

    private User admin;
    private User target;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1);
        admin.setEmail("admin@tukan.com");

        target = new User();
        target.setId(2);
        target.setEmail("user@tukan.com");
    }

    @Nested
    class DeleteSuccess {

        @Test
        void shouldDelegateToUserDeletionServiceWhenTargetExists() {
            // given
            when(userRepository.findByEmail("admin@tukan.com")).thenReturn(Optional.of(admin));
            when(userRepository.findById(2)).thenReturn(Optional.of(target));

            // when
            userService.delete(2, "admin@tukan.com");

            // then — deletion fully delegated; no inline cascade in UserService
            verify(userDeletionService).deleteUserAndOwnedData(2);
        }
    }

    @Nested
    class SelfDeletionGuard {

        @Test
        void shouldThrowForbiddenWhenAdminTriesToDeleteOwnAccount() {
            // given — authenticated user and target are the same
            when(userRepository.findByEmail("admin@tukan.com")).thenReturn(Optional.of(admin));
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));

            // when / then
            assertThatThrownBy(() -> userService.delete(1, "admin@tukan.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assert be.getStatus() == HttpStatus.FORBIDDEN;
                    });

            verify(userDeletionService, never()).deleteUserAndOwnedData(1);
        }
    }

    @Nested
    class TargetNotFound {

        @Test
        void shouldThrowNotFoundWhenTargetUserDoesNotExist() {
            // given
            when(userRepository.findByEmail("admin@tukan.com")).thenReturn(Optional.of(admin));
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> userService.delete(99, "admin@tukan.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assert be.getStatus() == HttpStatus.NOT_FOUND;
                    });

            verify(userDeletionService, never()).deleteUserAndOwnedData(99);
        }
    }
}
