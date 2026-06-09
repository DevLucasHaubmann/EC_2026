package com.tukan.api.service;

import com.tukan.api.entity.User;
import com.tukan.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for {@link UserService#search(String, Pageable)}: the service trims
 * the incoming term and delegates to the repository's parameterized query, returning
 * its page unchanged (pagination preserved).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceSearchTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private UserDeletionService userDeletionService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldTrimTermAndDelegateToRepository() {
        // given
        Pageable pageable = PageRequest.of(1, 5);
        Page<User> expected = new PageImpl<>(List.of(new User()), pageable, 7);
        when(userRepository.searchByNameOrEmail("ali", pageable)).thenReturn(expected);

        // when
        Page<User> result = userService.search("  ali  ", pageable);

        // then — trimmed term forwarded and the repository page returned as-is
        verify(userRepository).searchByNameOrEmail("ali", pageable);
        assertThat(result).isSameAs(expected);
        assertThat(result.getNumber()).isEqualTo(1);
    }
}
