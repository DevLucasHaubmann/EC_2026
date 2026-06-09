package com.tukan.api.repository;

import com.tukan.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence-level coverage for {@link UserRepository#searchByNameOrEmail} against a
 * JPA-managed schema (H2). Proves the new admin search behavior: case-insensitive partial
 * match on name OR email, preserved pagination, and parameter binding (no manual SQL
 * concatenation — an injection-style term is matched literally and finds nothing).
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void seedUsers() {
        persist("Alice Smith", "alice@tukan.com");
        persist("Bob Jones", "bob@example.com");
        persist("Carol White", "carol@tukan.com");
        entityManager.flush();
    }

    private void persist(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setType(User.UserType.USER);
        user.setStatus(User.UserState.ACTIVE);
        entityManager.persist(user);
    }

    @Test
    @DisplayName("matches by name regardless of case")
    void shouldMatchByNameCaseInsensitive() {
        Page<User> result = userRepository.searchByNameOrEmail("ALICE", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(User::getEmail)
                .containsExactly("alice@tukan.com");
    }

    @Test
    @DisplayName("matches by email fragment regardless of case")
    void shouldMatchByEmailCaseInsensitive() {
        Page<User> result = userRepository.searchByNameOrEmail("EXAMPLE.COM", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(User::getName)
                .containsExactly("Bob Jones");
    }

    @Test
    @DisplayName("matches both name and email hits and preserves pagination")
    void shouldPreservePaginationAcrossNameAndEmailMatches() {
        // "tukan" appears in two emails -> two total hits, paged one per page
        Page<User> firstPage = userRepository.searchByNameOrEmail("tukan", PageRequest.of(0, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);

        Page<User> secondPage = userRepository.searchByNameOrEmail("tukan", PageRequest.of(1, 1));
        assertThat(secondPage.getContent()).hasSize(1);

        assertThat(firstPage.getContent().get(0).getEmail())
                .isNotEqualTo(secondPage.getContent().get(0).getEmail());
    }

    @Test
    @DisplayName("treats an injection-style term as a literal value (parameter binding)")
    void shouldTreatInjectionTermLiterally() {
        Page<User> result = userRepository.searchByNameOrEmail("' OR '1'='1", PageRequest.of(0, 10));

        // If the term were concatenated into SQL this would leak all rows;
        // parameter binding matches it literally, so nothing is returned.
        assertThat(result.getTotalElements()).isZero();
    }
}
