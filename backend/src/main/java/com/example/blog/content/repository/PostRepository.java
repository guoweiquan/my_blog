package com.example.blog.content.repository;

import com.example.blog.content.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlugAndDeletedAtIsNull(String slug);

    Page<Post> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Page<Post> findByDeletedAtIsNull(Pageable pageable);

    boolean existsBySlug(String slug);
}
