package com.tukan.api.service;

import com.tukan.api.dto.UpdateUserRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers Bug B2 — an admin must not run destructive administrative actions against
 * their own account, while the same actions against other users keep working.
 * Targets {@link UserService#update} and {@link UserService#revokeAllSessions}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceSelfActionTest {

    private static final String ADMIN_EMAIL = "admin@tukan.com";

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
        admin.setEmail(ADMIN_EMAIL);
        admin.setType(User.UserType.ADMIN);
        admin.setStatus(User.UserState.ACTIVE);

        target = new User();
        target.setId(2);
        target.setEmail("user@tukan.com");
        target.setType(User.UserType.USER);
        target.setStatus(User.UserState.ACTIVE);
    }

    @Nested
    class DestructiveSelfUpdateGuard {

        @Test
        void shouldThrowForbiddenWhenAdminDemotesOwnRole() {
            // given — admin tries to remove their own ADMIN role
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));
            UpdateUserRequest request = new UpdateUserRequest(null, null, User.UserType.USER, null);

            // when / then
            assertForbidden(() -> userService.update(1, request, ADMIN_EMAIL));
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowForbiddenWhenAdminDeactivatesOwnAccount() {
            // given — admin tries to block their own account
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, User.UserState.BLOCKED);

            // when / then
            assertForbidden(() -> userService.update(1, request, ADMIN_EMAIL));
            verify(userRepository, never()).save(any());
            verify(userSessionService, never()).revokeAllSessions(1);
        }
    }

    @Nested
    class NonDestructiveSelfUpdateAllowed {

        @Test
        void shouldAllowAdminToUpdateOwnNameOnly() {
            // given — name change is non-destructive
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));
            when(userRepository.save(any())).thenReturn(admin);
            UpdateUserRequest request = new UpdateUserRequest("New Name", null, null, null);

            // when
            User result = userService.update(1, request, ADMIN_EMAIL);

            // then
            assertThat(result.getName()).isEqualTo("New Name");
            verify(userRepository).save(admin);
        }
    }

    @Nested
    class CrossUserUpdateStillWorks {

        @Test
        void shouldDeactivateAnotherUserAndRevokeTheirSessions() {
            // given — admin blocks a different user
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
            when(userRepository.findById(2)).thenReturn(Optional.of(target));
            when(userRepository.save(any())).thenReturn(target);
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, User.UserState.BLOCKED);

            // when
            userService.update(2, request, ADMIN_EMAIL);

            // then — destructive update on another user is allowed and forces logout
            verify(userRepository).save(target);
            verify(userSessionService).revokeAllSessions(2);
        }
    }

    @Nested
    class RevokeSessionsSelfGuard {

        @Test
        void shouldThrowForbiddenWhenAdminRevokesOwnSessions() {
            // given — authenticated admin targets their own id
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
            when(userRepository.existsById(1)).thenReturn(true);

            // when / then
            assertForbidden(() -> userService.revokeAllSessions(1, ADMIN_EMAIL));
            verify(userSessionService, never()).revokeAllSessions(1);
        }

        @Test
        void shouldThrowNotFoundWhenTargetUserDoesNotExist() {
            // given
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
            when(userRepository.existsById(99)).thenReturn(false);

            // when / then
            assertThatThrownBy(() -> userService.revokeAllSessions(99, ADMIN_EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
            verify(userSessionService, never()).revokeAllSessions(99);
        }

        @Test
        void shouldRevokeSessionsOfAnotherUser() {
            // given — target is a different user
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
            when(userRepository.existsById(2)).thenReturn(true);

            // when
            userService.revokeAllSessions(2, ADMIN_EMAIL);

            // then
            verify(userSessionService).revokeAllSessions(2);
        }
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
