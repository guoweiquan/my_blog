CREATE TABLE IF NOT EXISTS roles (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名（如ROLE_ADMIN）',
    description VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id INT UNSIGNED NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS categories (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    slug VARCHAR(100) NOT NULL UNIQUE COMMENT 'URL别名',
    parent_id INT UNSIGNED DEFAULT NULL COMMENT '父分类ID',
    description TEXT DEFAULT NULL COMMENT '分类描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_parent (parent_id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    slug VARCHAR(200) NOT NULL UNIQUE COMMENT 'URL别名',
    summary TEXT DEFAULT NULL COMMENT '摘要',
    content LONGTEXT NOT NULL COMMENT '正文',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    category_id INT UNSIGNED DEFAULT NULL COMMENT '分类ID',
    status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态',
    is_top TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
    author_id BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
    view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览量',
    like_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
    comment_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
    reading_time INT UNSIGNED DEFAULT NULL COMMENT '阅读时长',
    seo_keywords VARCHAR(500) DEFAULT NULL COMMENT 'SEO关键词',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    published_at DATETIME DEFAULT NULL COMMENT '发布时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    INDEX idx_author (author_id),
    INDEX idx_category (category_id),
    INDEX idx_status_published (status, published_at),
    INDEX idx_deleted (deleted_at),
    FULLTEXT INDEX idx_posts_fulltext (title, content) WITH PARSER ngram,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

CREATE TABLE IF NOT EXISTS tags (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名',
    description VARCHAR(200) DEFAULT NULL COMMENT '标签描述',
    post_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联文章数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

CREATE TABLE IF NOT EXISTS post_tags (
    post_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    tag_id INT UNSIGNED NOT NULL COMMENT '标签ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (post_id, tag_id),
    INDEX idx_tag (tag_id),
    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    post_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
    parent_id BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID',
    content TEXT NOT NULL COMMENT '评论内容',
    author_name VARCHAR(50) DEFAULT NULL COMMENT '匿名用户名',
    author_email VARCHAR(100) DEFAULT NULL COMMENT '匿名邮箱',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT 'IP地址',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_post (post_id),
    INDEX idx_user (user_id),
    INDEX idx_parent (parent_id),
    INDEX idx_status (status),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

CREATE TABLE IF NOT EXISTS likes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    post_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_like_user_post (user_id, post_id),
    INDEX idx_like_post (post_id),
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    post_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_fav_user_post (user_id, post_id),
    INDEX idx_fav_post (post_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '订阅ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    tag_id INT UNSIGNED NOT NULL COMMENT '标签ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sub_user_tag (user_id, tag_id),
    INDEX idx_sub_tag (tag_id),
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签订阅表';

CREATE TABLE IF NOT EXISTS site_settings (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '设置ID',
    setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    setting_value TEXT DEFAULT NULL COMMENT '配置值',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点配置表';

CREATE TABLE IF NOT EXISTS view_stats (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    post_id BIGINT UNSIGNED DEFAULT NULL COMMENT '文章ID',
    page_views INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'PV',
    unique_visitors INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'UV',
    UNIQUE KEY uk_view_stats_date_post (stat_date, post_id),
    INDEX idx_view_stats_date (stat_date),
    CONSTRAINT fk_view_stats_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问统计表';

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '审计ID',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    entity_id BIGINT UNSIGNED NOT NULL COMMENT '实体ID',
    changes JSON DEFAULT NULL COMMENT '变更内容',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT 'IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT 'UA',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_created (created_at),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

CREATE TABLE IF NOT EXISTS post_revisions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '修订ID',
    post_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content LONGTEXT NOT NULL COMMENT '正文',
    revised_by BIGINT UNSIGNED NOT NULL COMMENT '修订人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_revision_post (post_id),
    INDEX idx_revision_created (created_at),
    CONSTRAINT fk_post_revisions_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_revisions_user FOREIGN KEY (revised_by) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章修订历史表';

INSERT INTO roles (name, description)
VALUES ('ROLE_ADMIN', '管理员'), ('ROLE_USER', '普通用户')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO site_settings (setting_key, setting_value, description)
VALUES
    ('site_title', '我的博客', '站点标题'),
    ('site_subtitle', '分享技术与生活', '站点副标题'),
    ('site_description', '这是一个基于Vue3和Spring Boot构建的博客系统', '站点描述'),
    ('comment_need_review', '1', '评论是否需要审核：1是，0否')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), description = VALUES(description);
