package com.example.blog.content.repository;

import com.example.blog.content.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlugAndDeletedAtIsNull(String slug);

    Page<Post> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Page<Post> findByDeletedAtIsNull(Pageable pageable);

    boolean existsBySlug(String slug);

    long countByStatusAndDeletedAtIsNull(String status);

    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL AND LOWER(p.status) = 'published' " +
            "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.summary) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchPublished(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM posts WHERE deleted_at IS NULL AND LOWER(status) = 'published' " +
            "AND MATCH(title, summary, content) AGAINST (:keyword IN BOOLEAN MODE)",
            countQuery = "SELECT COUNT(*) FROM posts WHERE deleted_at IS NULL AND LOWER(status) = 'published' " +
                    "AND MATCH(title, summary, content) AGAINST (:keyword IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Post> searchPublishedFulltext(@Param("keyword") String keyword, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = COALESCE(p.viewCount, 0) + :delta WHERE p.id = :postId")
    void increaseViewCount(@Param("postId") Long postId, @Param("delta") long delta);
}
