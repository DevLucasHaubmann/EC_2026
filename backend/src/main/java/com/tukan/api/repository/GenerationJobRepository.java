package com.tukan.api.repository;

import com.tukan.api.entity.GenerationJob;
import com.tukan.api.entity.GenerationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query methods marked as preparatory for upcoming async-job tasks (2.3D+).
 * Contract for the {@code statusIn} methods: callers must pass a non-empty collection —
 * an empty collection yields an empty SQL {@code IN ()} predicate.
 */
public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {

    Optional<GenerationJob> findByIdAndUserId(UUID id, Integer userId);

    boolean existsByUserIdAndStatusIn(Integer userId, Collection<GenerationJobStatus> statuses);

    Optional<GenerationJob> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
            Integer userId, Collection<GenerationJobStatus> statuses);

    List<GenerationJob> findByStatusInAndUpdatedAtBefore(
            Collection<GenerationJobStatus> statuses, Instant threshold);

    @Modifying(clearAutomatically = true)
    @Query("delete from GenerationJob j where j.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
