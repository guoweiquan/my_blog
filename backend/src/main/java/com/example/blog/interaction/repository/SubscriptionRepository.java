package com.example.blog.interaction.repository;

import com.example.blog.interaction.entity.Subscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndTagId(Long userId, Long tagId);

    long countByUserId(Long userId);

    List<Subscription> findByUserId(Long userId);
}
