package com.tukan.api.repository;

import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.Recommendation.RecommendationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Integer> {

    @EntityGraph(attributePaths = "user")
    Optional<Recommendation> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(Integer userId, List<RecommendationStatus> statuses);

    @EntityGraph(attributePaths = "user")
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Integer userId);

    @EntityGraph(attributePaths = "user")
    Optional<Recommendation> findByIdAndUserId(Integer id, Integer userId);

    List<Recommendation> findByUserIdAndStatusIn(Integer userId, List<RecommendationStatus> statuses);

    @Override
    @EntityGraph(attributePaths = "user")
    @NonNull
    Optional<Recommendation> findById(@NonNull Integer id);

    @Modifying(clearAutomatically = true)
    @Query("delete from Recommendation r where r.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}