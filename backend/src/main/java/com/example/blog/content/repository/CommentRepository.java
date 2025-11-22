package com.example.blog.content.repository;

import com.example.blog.content.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, String status);

    Long countByPostIdAndStatus(Long postId, String status);
}
