package com.tukan.api.repository;

import com.tukan.api.entity.BodyWeightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BodyWeightLogRepository extends JpaRepository<BodyWeightLog, Long> {

    // Chronological order by creation instant (with id as a deterministic tie-breaker for
    // multiple registrations within the same instant), so the full history — including more
    // than one entry per day — is returned in the order the user recorded it.
    List<BodyWeightLog> findByUserIdOrderByCreatedAtAscIdAsc(Integer userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from BodyWeightLog b where b.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
