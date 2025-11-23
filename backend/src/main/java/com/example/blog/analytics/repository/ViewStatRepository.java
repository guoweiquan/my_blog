package com.example.blog.analytics.repository;

import com.example.blog.analytics.entity.ViewStat;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewStatRepository extends JpaRepository<ViewStat, Long> {

    Optional<ViewStat> findByStatDateAndPostId(LocalDate statDate, Long postId);

    Optional<ViewStat> findByStatDateAndPostIsNull(LocalDate statDate);
}
