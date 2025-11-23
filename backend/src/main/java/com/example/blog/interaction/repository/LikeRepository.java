package com.example.blog.interaction.repository;

import com.example.blog.interaction.entity.LikeRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<LikeRecord, Long> {

    Optional<LikeRecord> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);
}
