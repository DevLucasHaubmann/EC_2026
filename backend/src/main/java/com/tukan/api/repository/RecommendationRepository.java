package com.tukan.api.repository;

import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.Recommendation.RecommendationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Integer> {

    @EntityGraph(attributePaths = "user")
    Optional<Recommendation> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);

    @EntityGraph(attributePaths = "user")
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Integer userId);

    @EntityGraph(attributePaths = "user")
    Optional<Recommendation> findByIdAndUserId(Integer id, Integer userId);

    List<Recommendation> findByUserIdAndStatusIn(Integer userId, List<RecommendationStatus> statuses);

    @Override
    @EntityGraph(attributePaths = "user")
    @NonNull
    Optional<Recommendation> findById(@NonNull Integer id);
}