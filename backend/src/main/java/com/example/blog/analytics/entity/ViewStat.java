package com.example.blog.analytics.entity;

import com.example.blog.content.entity.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "view_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_view_stats_date_post", columnNames = {"stat_date", "post_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ViewStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "page_views", nullable = false)
    private Integer pageViews = 0;

    @Column(name = "unique_visitors", nullable = false)
    private Integer uniqueVisitors = 0;
}
