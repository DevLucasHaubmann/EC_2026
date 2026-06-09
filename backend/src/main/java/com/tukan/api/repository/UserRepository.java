package com.tukan.api.repository;

import com.tukan.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Admin search: case-insensitive partial match on name OR email, paginated.
     * The caller is responsible for passing a non-blank, trimmed term.
     */
    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    Page<User> searchByNameOrEmail(@Param("term") String term, Pageable pageable);
}