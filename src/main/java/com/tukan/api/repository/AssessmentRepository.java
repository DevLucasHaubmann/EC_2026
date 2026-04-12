package com.tukan.api.repository;

import com.tukan.api.entity.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {

    boolean existsByUserId(Integer userId);

    @EntityGraph(attributePaths = "user")
    Optional<Assessment> findByUserId(Integer userId);

    @Override
    @EntityGraph(attributePaths = "user")
    @NonNull
    Optional<Assessment> findById(@NonNull Integer id);

    @Override
    @EntityGraph(attributePaths = "user")
    @NonNull
    Page<Assessment> findAll(@NonNull Pageable pageable);

    void deleteByUserId(Integer userId);
}