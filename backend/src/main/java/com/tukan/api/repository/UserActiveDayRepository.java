package com.tukan.api.repository;

import com.tukan.api.entity.UserActiveDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UserActiveDayRepository extends JpaRepository<UserActiveDay, Long> {

    boolean existsByUserIdAndActiveDate(Integer userId, LocalDate date);

    @Query("select u.activeDate from UserActiveDay u where u.user.id = :userId order by u.activeDate desc")
    List<LocalDate> findActiveDatesByUserId(@Param("userId") Integer userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from UserActiveDay u where u.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
