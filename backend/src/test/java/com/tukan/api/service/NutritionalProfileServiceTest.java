package com.tukan.api.service;

import com.tukan.api.dto.CreateProfileRequest;
import com.tukan.api.dto.UpdateProfileRequest;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.NutritionalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NutritionalProfileServiceTest {

    @Mock
    private NutritionalProfileRepository nutritionalProfileRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private NutritionalProfileService nutritionalProfileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("user@test.com");
    }

    private CreateProfileRequest validCreateRequest() {
        return new CreateProfileRequest(
                LocalDate.of(1995, 6, 15),
                NutritionalProfile.Gender.MALE,
                80.0,
                175.0,
                NutritionalProfile.ActivityLevel.MODERATE
        );
    }

    @Nested
    class CreateOwn {

        @Test
        void shouldCreateProfileWithAllRequiredFieldsSuccessfully() {
            // given
            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(false);
            when(nutritionalProfileRepository.save(any(NutritionalProfile.class))).thenAnswer(inv -> {
                NutritionalProfile p = inv.getArgument(0);
                p.setId(1);
                return p;
            });

            // when
            NutritionalProfile result = nutritionalProfileService.createOwn("user@test.com", validCreateRequest());

            // then
            assertThat(result.getGender()).isEqualTo(NutritionalProfile.Gender.MALE);
            assertThat(result.getWeightKg()).isEqualTo(80.0);
            assertThat(result.getHeightCm()).isEqualTo(175.0);
            assertThat(result.getActivityLevel()).isEqualTo(NutritionalProfile.ActivityLevel.MODERATE);
            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 6, 15));
            assertThat(result.getUser()).isEqualTo(user);
            verify(nutritionalProfileRepository).save(any(NutritionalProfile.class));
        }

        @Test
        void shouldThrowConflictWhenProfileAlreadyExists() {
            // given
            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(true);

            // when / then
            assertThatThrownBy(() -> nutritionalProfileService.createOwn("user@test.com", validCreateRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já possui um perfil");
        }
    }

    @Nested
    class FindOwn {

        @Test
        void shouldReturnProfileForAuthenticatedUser() {
            // given
            NutritionalProfile profile = new NutritionalProfile();
            profile.setId(1);
            profile.setUser(user);
            profile.setGender(NutritionalProfile.Gender.FEMALE);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(nutritionalProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

            // when
            NutritionalProfile result = nutritionalProfileService.findOwn("user@test.com");

            // then
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getGender()).isEqualTo(NutritionalProfile.Gender.FEMALE);
        }

        @Test
        void shouldThrowNotFoundWhenProfileDoesNotExist() {
            // given
            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(nutritionalProfileRepository.findByUserId(1)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> nutritionalProfileService.findOwn("user@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Perfil não encontrado");
        }
    }

    @Nested
    class UpdateOwn {

        @Test
        void shouldUpdateWeightWhenOnlyWeightIsProvided() {
            // given
            NutritionalProfile profile = new NutritionalProfile();
            profile.setId(1);
            profile.setUser(user);
            profile.setWeightKg(80.0);

            UpdateProfileRequest request = new UpdateProfileRequest(85.0, null, null);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(nutritionalProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
            when(nutritionalProfileRepository.save(any(NutritionalProfile.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            NutritionalProfile result = nutritionalProfileService.updateOwn("user@test.com", request);

            // then
            assertThat(result.getWeightKg()).isEqualTo(85.0);
        }

        @Test
        void shouldThrowBadRequestWhenAllFieldsAreNull() {
            // given
            UpdateProfileRequest emptyRequest = new UpdateProfileRequest(null, null, null);

            // when / then
            assertThatThrownBy(() -> nutritionalProfileService.updateOwn("user@test.com", emptyRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pelo menos um campo");
        }

        @Test
        void shouldThrowNotFoundWhenProfileDoesNotExistOnUpdate() {
            // given
            UpdateProfileRequest request = new UpdateProfileRequest(70.0, null, null);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(nutritionalProfileRepository.findByUserId(1)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> nutritionalProfileService.updateOwn("user@test.com", request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Perfil não encontrado");
        }
    }
}
