package com.tukan.api.repository;

import com.tukan.api.entity.NutritionalProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface NutritionalProfileRepository extends JpaRepository<NutritionalProfile, Integer> {

    boolean existsByUserId(Integer userId);

    @EntityGraph(attributePaths = "user")
    Optional<NutritionalProfile> findByUserId(Integer userId);

    @Override
    @EntityGraph(attributePaths = "user")
    @NonNull
    Optional<NutritionalProfile> findById(@NonNull Integer id);

    @Override
    @EntityGraph(attributePaths = "user")
    @NonNull
    Page<NutritionalProfile> findAll(@NonNull Pageable pageable);

    void deleteByUserId(Integer userId);
}