package com.tukan.api.service;

import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeServiceAvatarTest {

    @Mock
    private UserService userService;

    @Mock
    private NutritionalProfileRepository nutritionalProfileRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeService meService;

    private static final String EMAIL = "user@example.com";
    private static final Integer USER_ID = 1;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);

        when(userService.findByEmail(EMAIL)).thenReturn(user);
    }

    // -------------------------------------------------------------------------
    // updateAvatar — URLs aceitas
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateAvatar — URLs aceitas")
    class AcceptedUrls {

        @BeforeEach
        void stubSave() {
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(nutritionalProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(assessmentRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("aceita URL https válida")
        void accepts_https_url() {
            // given / when
            var data = meService.updateAvatar(EMAIL, "https://cdn.example.com/avatar.png");

            // then
            assertThat(data.user().getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
        }

        @Test
        @DisplayName("aceita URL http válida (ambiente local)")
        void accepts_http_url() {
            // given / when
            var data = meService.updateAvatar(EMAIL, "http://localhost:8080/avatar.png");

            // then
            assertThat(data.user().getAvatarUrl()).isEqualTo("http://localhost:8080/avatar.png");
        }

        @Test
        @DisplayName("aceita null — limpa o avatar")
        void accepts_null_clears_avatar() {
            // given
            user.setAvatarUrl("https://cdn.example.com/old.png");

            // when
            var data = meService.updateAvatar(EMAIL, null);

            // then
            assertThat(data.user().getAvatarUrl()).isNull();
        }

        @Test
        @DisplayName("aceita string em branco — limpa o avatar")
        void accepts_blank_clears_avatar() {
            // given
            user.setAvatarUrl("https://cdn.example.com/old.png");

            // when
            var data = meService.updateAvatar(EMAIL, "   ");

            // then
            assertThat(data.user().getAvatarUrl()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // updateAvatar — URLs rejeitadas
    // Rejected paths throw BusinessException before reaching userRepository.save
    // and the profile/assessment repos, so shared stubs are not consumed.
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateAvatar — URLs rejeitadas")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class RejectedUrls {

        @Test
        @DisplayName("rejeita javascript: — lança BusinessException")
        void rejects_javascript_scheme() {
            assertThatThrownBy(() -> meService.updateAvatar(EMAIL, "javascript:alert(1)"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("rejeita data: — lança BusinessException")
        void rejects_data_scheme() {
            assertThatThrownBy(() -> meService.updateAvatar(EMAIL, "data:image/png;base64,abc123"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("rejeita file: — lança BusinessException")
        void rejects_file_scheme() {
            assertThatThrownBy(() -> meService.updateAvatar(EMAIL, "file:///etc/passwd"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("rejeita URL sem protocolo (www.x.com) — lança BusinessException")
        void rejects_url_without_protocol() {
            assertThatThrownBy(() -> meService.updateAvatar(EMAIL, "www.example.com/avatar.png"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("rejeita ftp: — lança BusinessException")
        void rejects_ftp_scheme() {
            assertThatThrownBy(() -> meService.updateAvatar(EMAIL, "ftp://files.example.com/avatar.png"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAuthenticatedUserData — avatarUrl exposto via MeResponse
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAuthenticatedUserData — avatarUrl presente")
    class AvatarInResponse {

        @BeforeEach
        void stubRepos() {
            when(nutritionalProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(assessmentRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("avatarUrl do usuário é incluído nos dados retornados")
        void avatarUrl_presentInAuthenticatedData() {
            // given
            user.setAvatarUrl("https://cdn.example.com/avatar.png");

            // when
            var data = meService.findAuthenticatedUserData(EMAIL);

            // then
            assertThat(data.user().getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
        }
    }
}
