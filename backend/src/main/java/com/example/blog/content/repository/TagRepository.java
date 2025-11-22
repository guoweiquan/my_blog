package com.example.blog.content.repository;

import com.example.blog.content.entity.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByIdIn(Collection<Long> ids);

    Optional<Tag> findByName(String name);
}
