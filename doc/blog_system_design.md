# 博客系统设计方案（Vue 3 + Spring Boot + MySQL + Redis）

## 1. 项目概述
- 目标：构建一个支持文章发布、阅读、评论、标签分类及基础运营统计的个人博客系统。
- 技术栈：
  - 前端：Vue 3（组合式 API + Vite + Pinia + Vue Router）
  - 后端：Spring Boot 3.x（Java 17）、Spring Web, Spring Security, Spring Data JPA
  - 数据库：MySQL 8.0+（生产环境）/ H2（开发测试）
  - 缓存：Redis 7.0+（多级缓存、Session、限流）
  - API 风格：RESTful + JWT 认证
- 部署形态：
  - 单体应用，反向代理（Nginx）托管前端静态资源与后端 API 转发
  - 支持 Docker Compose 一键部署
  - 云主机或本地服务器均可快速落地

## 2. 核心需求与用户角色

### 2.1 用户角色
- 访客：浏览文章、评论、点赞、搜索
- 注册用户：除访客权限外，可收藏文章、订阅标签
- 管理员：文章增删改、评论审核、标签管理、运营数据查看

### 2.2 功能列表
- 文章管理：
  - 创建、编辑、删除文章
  - 设置发布状态（草稿/已发布）
  - 支持 Markdown 编辑与预览
  - 封面图片上传
- 分类与标签：
  - 文章多标签关联
  - 可选分类字段
  - 标签订阅（用户关注某些标签）
- 评论系统：
  - 访客和注册用户均可评论（可配置）
  - 管理员审核评论（防垃圾）
  - 支持嵌套回复（楼中楼）
  - 简单防刷机制（频率限制）
- 用户系统：
  - 用户注册、登录、退出
  - 找回密码（预留，初版可仅管理员重置）
  - 基于 JWT 的鉴权
  - 角色权限控制（管理员/普通用户）
- 搜索与推荐：
  - 标题/内容关键字搜索
  - 按标签搜索
  - 按热度（阅读量/点赞数）排序
- 统计与仪表盘：
  - 页面访问量（PV）统计
  - 独立访客数（UV）（以 IP/UserId 粗略估算）
  - 文章阅读量、评论量统计
  - 热门标签、热门文章
- 设置与运营：
  - 站点信息（标题、副标题、介绍、Logo）
  - 公告管理
  - 友情链接
  - 社交媒体链接（GitHub、微博等）
- 扩展能力（后续升级）：
  - RSS 输出
  - Webhooks（新文章发布时推送）
  - 第三方登录（GitHub/微信/QQ 等）

## 3. 整体架构设计

### 3.1 架构风格
- 前后端分离
  - 前端构建后部署为静态资源
  - 后端提供 RESTful API，统一前缀 `/api`
- 网络拓扑：
  - Nginx：反向代理 + 静态资源服务器 + 负载均衡
  - Spring Boot：运行在 8080 端口（支持多实例）
  - MySQL：主数据库（8.0+），支持读写分离扩展
  - Redis：缓存层 + Session 存储 + 分布式锁
- 安全与认证：
  - 使用 JWT 双 Token 机制（Access Token + Refresh Token）
  - Access Token 存储在内存（Pinia Store），有效期 15 分钟
  - Refresh Token 存储在 HttpOnly Cookie，有效期 7 天
  - 每次请求通过 `Authorization: Bearer <token>` 头传递 Access Token
  - Token 黑名单存储在 Redis（用于登出和强制下线）
- 缓存策略（Redis 单层缓存）：
  - **缓存内容**：
    - 文章详情（5 分钟 TTL）
    - 文章列表（按分页条件缓存，10 分钟 TTL）
    - 标签列表（10 分钟 TTL）
    - 用户信息（30 分钟 TTL）
    - 站点配置（30 分钟 TTL）
    - 统计数据（实时计数缓冲，定期刷入数据库）
  - **缓存更新策略**：
    - 写操作主动失效相关缓存（Cache-Aside 模式）
    - 删除缓存而非更新缓存，避免并发问题
    - 支持多实例部署时的缓存共享
- 日志与监控：
  - Spring Boot Actuator + Prometheus 监控指标
  - 应用日志结构化输出（JSON 格式）
  - 慢查询日志（MySQL slow query log）
  - Redis 监控（内存使用、命中率）
  - Nginx Access Log 记录前端访问

### 3.2 后端模块划分
- `auth` 模块：
  - 用户注册、登录、注销
  - JWT 生成与校验
  - 权限控制（基于角色）
- `content` 模块：
  - 文章管理（增删改查）
  - 标签管理
  - 评论管理
- `interaction` 模块：
  - 点赞、收藏、订阅标签
- `analytics` 模块：
  - 访问统计数据收集与聚合
  - 仪表盘数据接口
- `common` 模块：
  - 全局配置
  - 错误码与异常处理
  - 文件上传与访问（如头像、封面）

## 4. 前端设计（Vue 3）

### 4.1 技术选型
- 构建工具：Vite
- 框架：Vue 3（组合式 API）
- 路由：Vue Router 4
- 状态管理：Pinia
- UI 组件库：推荐 Element Plus 或 Ant Design Vue（二选一）
- Markdown 编辑器：`@kangc/v-md-editor` 等现成组件
- HTTP 库：Axios

### 4.2 路由与页面结构
- 公共页面：
  - `/`：首页
    - 最新文章列表
    - 热门文章推荐
    - 热门标签展示
    - 公告栏
  - `/post/:slug`：文章详情
    - 标题、作者、发布时间
    - 正文（Markdown 渲染）
    - 标签
    - 阅读量、点赞数
    - 评论列表 & 评论表单
  - `/tag/:tagName`：标签文章列表
  - `/archive`：归档（按年份/月份分组）
  - `/search`：搜索页面（支持关键词、标签）
- 用户相关页面：
  - `/login`：登录
  - `/register`：注册
  - `/me`：个人中心
    - 基本信息修改
    - 我的收藏
    - 我的评论（可选）
- 管理后台页面（需管理员权限）：
  - `/admin`：仪表盘
    - 总 PV、UV
    - 总文章数、评论数
    - 热门文章 TOP N
  - `/admin/posts`：文章管理
    - 列表（搜索、过滤、分页）
    - 新建/编辑文章页面（支持 Markdown 编辑）
  - `/admin/comments`：评论审核
  - `/admin/tags`：标签维护
  - `/admin/settings`：站点设置

### 4.3 前端状态管理（Pinia Store）
- `useAuthStore`：
  - 状态：当前用户信息、JWT Token、登录状态
  - 动作：登录、退出登录、获取当前用户信息
- `usePostStore`：
  - 状态：文章列表、当前文章、分页信息、过滤条件
  - 动作：获取文章列表、获取文章详情、创建/编辑/删除文章
- `useTagStore`：
  - 状态：标签列表、热门标签
  - 动作：获取标签列表、创建/编辑/删除标签（管理员）
- `useDashboardStore`：
  - 状态：仪表盘统计数据
  - 动作：获取 PV/UV、热门文章、热门标签等

### 4.4 请求封装
- 使用 Axios 封装统一请求模块：
  - 请求拦截器：自动注入 JWT Token
  - 响应拦截器：错误统一处理（如未授权自动跳转登录）
- 按功能划分 API 模块，例如：
  - `api/auth.ts`
  - `api/post.ts`
  - `api/comment.ts`
  - `api/tag.ts`
  - `api/dashboard.ts`

## 5. 后端设计（Spring Boot）

### 5.1 主要依赖
- Spring Boot 3.x
- Spring Web
- Spring Security + JWT（jjwt 库）
- Spring Data JPA + Hibernate
- Spring Data Redis（RedisTemplate + Lettuce 客户端）
- Spring Cache（集成 Redis 缓存）
- MySQL Connector/J 8.0+
- Flyway（数据库版本管理与迁移）
- Lombok（简化代码）
- MapStruct（DTO 与 Entity 转换）
- Hibernate Validator（参数校验）
- Redisson（分布式锁，可选）

### 5.2 分层结构
- Controller 层：
  - 定义 REST API
  - 仅负责参数接收和结果封装
- Service 层：
  - 业务逻辑核心
  - 事务控制（`@Transactional`）
- Repository 层：
  - 使用 Spring Data JPA 定义接口
- Domain/Entity 层：
  - JPA 实体类，对应数据库表
- DTO/VO 层：
  - 请求对象（Request DTO）
  - 响应对象（Response VO/DTO）
- Security 层：
  - 安全配置类（`SecurityConfig`）
  - JWT 过滤器
  - 自定义 `UserDetailsService`
- Common 层：
  - 统一异常处理（`@ControllerAdvice`）
  - 全局返回结果封装

### 5.3 核心业务点设计

#### 5.3.1 用户与认证（Auth）
- 注册：
  - 校验用户名、邮箱唯一性
  - 密码使用 BCrypt 加密存储（strength=10）
  - 发送验证邮件（可选，使用 Spring Mail）
- 登录（双 Token 机制）：
  - 校验用户名/邮箱 + 密码
  - 生成 Access Token（有效期 15 分钟，存储用户 ID、角色等信息）
  - 生成 Refresh Token（有效期 7 天，UUID 格式）
  - Refresh Token 存入 Redis：`refresh_token:{userId}:{tokenId}` → 7 天 TTL
  - 返回 Access Token（响应体）+ Refresh Token（HttpOnly Cookie）
- Token 刷新：
  - 前端检测 Access Token 即将过期，调用 `/api/auth/refresh`
  - 后端校验 Refresh Token（从 Cookie 获取，验证 Redis 存在性）
  - 签发新的 Access Token 返回
- 登出：
  - 将 Refresh Token 加入黑名单（Redis，TTL = 剩余有效期）
  - 清除 Cookie
- 强制下线：
  - 管理员可删除指定用户的所有 Refresh Token（Redis 批量删除）
- 权限控制：
  - 使用 Spring Security 的 `@PreAuthorize` 注解
  - 管理员角色：`ROLE_ADMIN`
  - 普通用户：`ROLE_USER`

#### 5.3.2 文章管理（Posts）
- 功能：
  - 新建/编辑/删除文章（管理员）
  - 设置文章状态：草稿/已发布
  - 设置封面图片 URL
- 查询：
  - 首页文章列表：只返回已发布的文章，按发布时间倒序
  - 按标签、关键词等过滤
  - 返回数据带有基本统计字段：阅读量、点赞数、评论数

#### 5.3.3 评论系统（Comments）
- 用户或访客提交评论：
  - 配置决定是否允许匿名评论
  - 可配置对所有评论先审核再显示
- 评论结构：
  - 支持父子评论关系（`parent_id`），实现楼中楼显示
- 评论管理：
  - 管理员可删除评论、修改状态（通过/拒绝）
  - 对敏感词、垃圾评论可后续扩展

#### 5.3.4 点赞与收藏（Likes & Favorites）
- 点赞：
  - 用户对文章点/取消赞
  - 可简单用一张 `likes` 表记录用户-文章关系
- 收藏：
  - 用户收藏文章
  - 在个人中心查看收藏列表

#### 5.3.5 统计与仪表盘（Analytics）
- 数据来源：
  - **阅读量统计**：
    - 用户访问文章详情时，增加 Redis 计数器：`post:view:{postId}`
    - 定时任务（每 5 分钟）批量刷入 MySQL `posts.view_count`
  - **UV 统计**：
    - 使用 Redis HyperLogLog 记录：`uv:daily:{date}` 和 `uv:post:{postId}:{date}`
    - 每日定时任务聚合到 `view_stats` 表
  - **实时在线用户数**：
    - 用户活跃时更新 Redis：`online:user:{userId}` → 5 分钟 TTL
    - 统计 Redis Key 数量获取在线人数
- 展示内容：
  - 总 PV/UV（从 MySQL 聚合）
  - 今日 PV/UV（从 Redis 实时读取）
  - 按日期聚合的访问趋势
  - 阅读量最高的文章 TOP 10（Redis ZSet 排行榜）
  - 使用次数最多的标签（定期从 MySQL 聚合）
- 缓存策略：
  - 仪表盘数据缓存 5 分钟（Redis）
  - 使用后台任务预热热门数据

## 6. API 设计示例

### 6.1 认证模块
- `POST /api/auth/register`
  - 请求：用户名、邮箱、密码
  - 响应：注册结果
- `POST /api/auth/login`
  - 请求：用户名/邮箱 + 密码
  - 响应：Access Token（响应体）+ Refresh Token（Set-Cookie）
- `POST /api/auth/refresh`
  - 请求：Refresh Token（Cookie 自动携带）
  - 响应：新的 Access Token
- `POST /api/auth/logout`
  - 将 Refresh Token 加入黑名单（Redis）
  - 清除 Cookie
- `GET /api/auth/profile`
  - 获取当前登录用户信息（从 Redis 缓存读取）
- `PUT /api/auth/profile`
  - 修改昵称、头像等（更新后失效缓存）

### 6.2 文章模块
- `GET /api/posts`
  - 查询条件：`page`，`size`，`keyword`，`tag`，`sort`
  - 返回文章列表（分页，Redis 缓存 10 分钟）
- `GET /api/posts/{slug}`
  - 返回文章详情（Redis 缓存 5 分钟）
  - 异步增加阅读量（Redis 计数器）
- `POST /api/posts`（管理员）
  - 新建文章
  - 失效文章列表缓存
- `PUT /api/posts/{id}`（管理员）
  - 更新文章
  - 失效相关缓存（详情 + 列表）
- `DELETE /api/posts/{id}`（管理员）
  - 软删除文章（设置 deleted_at 字段）
  - 失效所有相关缓存
- `POST /api/posts/{id}/like`
  - 当前用户对文章点赞/取消赞
  - Redis Set 记录点赞关系：`post:likes:{postId}`
  - Redis 计数器：`post:like_count:{postId}`
- `POST /api/posts/{id}/favorite`
  - 当前用户收藏/取消收藏文章
  - 同步更新 MySQL + 失效缓存

### 6.3 评论模块
- `GET /api/posts/{id}/comments`
  - 获取文章的评论列表（树结构）
- `POST /api/posts/{id}/comments`
  - 新增评论（需要登录或允许匿名）
- `DELETE /api/comments/{id}`（管理员）
  - 删除评论
- `PUT /api/comments/{id}/approve`（管理员）
  - 审核通过评论

### 6.4 标签与统计模块
- `GET /api/tags`
  - 获取所有标签 / 热门标签
- `POST /api/tags`（管理员）
  - 新增标签
- `PUT /api/tags/{id}`（管理员）
  - 修改标签
- `DELETE /api/tags/{id}`（管理员）
  - 删除标签
- `GET /api/dashboard/overview`（管理员）
  - 返回仪表盘统计数据

## 7. 数据库设计（MySQL 8.0+）

### 7.1 表结构概览
- `users`：用户信息
- `roles`：角色表
- `user_roles`：用户-角色关联表
- `posts`：文章表
- `categories`：分类表（支持树形结构）
- `tags`：标签表
- `post_tags`：文章-标签多对多关联
- `comments`：评论表
- `likes`：点赞记录表
- `favorites`：收藏记录表
- `subscriptions`：标签订阅记录
- `site_settings`：站点配置
- `view_stats`：访问统计表（按日聚合）
- `audit_logs`：审计日志表
- `post_revisions`：文章修订历史表（版本控制）

### 7.2 核心表 DDL（MySQL）

#### `users` - 用户表
```sql
CREATE TABLE `users` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
  `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_email` (`email`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

#### `roles` - 角色表
```sql
CREATE TABLE `roles` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
  `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名（如ROLE_ADMIN）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 初始化角色数据
INSERT INTO `roles` (`name`, `description`) VALUES 
('ROLE_ADMIN', '管理员'),
('ROLE_USER', '普通用户');
```

#### `user_roles` - 用户角色关联表
```sql
CREATE TABLE `user_roles` (
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id` INT UNSIGNED NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';
```

#### `categories` - 分类表
```sql
CREATE TABLE `categories` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `slug` VARCHAR(100) NOT NULL UNIQUE COMMENT 'URL别名',
  `parent_id` INT UNSIGNED DEFAULT NULL COMMENT '父分类ID（支持子分类）',
  `description` TEXT DEFAULT NULL COMMENT '分类描述',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_parent` (`parent_id`),
  CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';
```

#### `posts` - 文章表
```sql
CREATE TABLE `posts` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `slug` VARCHAR(200) NOT NULL UNIQUE COMMENT 'URL别名（SEO友好）',
  `summary` TEXT DEFAULT NULL COMMENT '摘要',
  `content` LONGTEXT NOT NULL COMMENT '正文（Markdown格式）',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
  `category_id` INT UNSIGNED DEFAULT NULL COMMENT '分类ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft草稿，published已发布',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1是，0否',
  `author_id` BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
  `reading_time` INT UNSIGNED DEFAULT NULL COMMENT '预估阅读时长（分钟）',
  `seo_keywords` VARCHAR(500) DEFAULT NULL COMMENT 'SEO关键词',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  INDEX `idx_author` (`author_id`),
  INDEX `idx_category` (`category_id`),
  INDEX `idx_status_published` (`status`, `published_at` DESC),
  INDEX `idx_deleted` (`deleted_at`),
  FULLTEXT INDEX `idx_fulltext` (`title`, `content`) WITH PARSER ngram,
  CONSTRAINT `fk_posts_author` FOREIGN KEY (`author_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_posts_category` FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';
```

#### `tags` - 标签表
```sql
CREATE TABLE `tags` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
  `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '标签描述',
  `post_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文章数量（冗余字段）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';
```

#### `post_tags` - 文章标签关联表
```sql
CREATE TABLE `post_tags` (
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
  `tag_id` INT UNSIGNED NOT NULL COMMENT '标签ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`post_id`, `tag_id`),
  INDEX `idx_tag` (`tag_id`),
  CONSTRAINT `fk_post_tags_post` FOREIGN KEY (`post_id`) REFERENCES `posts`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';
```

#### `comments` - 评论表
```sql
CREATE TABLE `comments` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID（NULL表示匿名）',
  `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID（NULL表示顶级评论）',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT '匿名用户名',
  `author_email` VARCHAR(100) DEFAULT NULL COMMENT '匿名邮箱',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending待审核，approved已通过，rejected已拒绝',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_post` (`post_id`),
  INDEX `idx_user` (`user_id`),
  INDEX `idx_parent` (`parent_id`),
  INDEX `idx_status` (`status`),
  CONSTRAINT `fk_comments_post` FOREIGN KEY (`post_id`) REFERENCES `posts`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `comments`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
```

#### `likes` - 点赞记录表
```sql
CREATE TABLE `likes` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
  INDEX `idx_post` (`post_id`),
  CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_likes_post` FOREIGN KEY (`post_id`) REFERENCES `posts`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞记录表';
```

#### `favorites` - 收藏记录表
```sql
CREATE TABLE `favorites` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
  INDEX `idx_post` (`post_id`),
  CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorites_post` FOREIGN KEY (`post_id`) REFERENCES `posts`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏记录表';
```

#### `subscriptions` - 标签订阅表
```sql
CREATE TABLE `subscriptions` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '订阅ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `tag_id` INT UNSIGNED NOT NULL COMMENT '标签ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`),
  INDEX `idx_tag` (`tag_id`),
  CONSTRAINT `fk_subscriptions_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_subscriptions_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签订阅表';
```

#### `site_settings` - 站点配置表
```sql
CREATE TABLE `site_settings` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '设置ID',
  `setting_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
  `setting_value` TEXT DEFAULT NULL COMMENT '配置值',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点配置表';

-- 初始化配置
INSERT INTO `site_settings` (`setting_key`, `setting_value`, `description`) VALUES
('site_title', '我的博客', '站点标题'),
('site_subtitle', '分享技术与生活', '站点副标题'),
('site_description', '这是一个基于Vue3和Spring Boot构建的博客系统', '站点描述'),
('comment_need_review', '1', '评论是否需要审核：1是，0否');
```

#### `view_stats` - 访问统计表
```sql
CREATE TABLE `view_stats` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '文章ID（NULL表示全站）',
  `page_views` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '页面浏览量',
  `unique_visitors` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '独立访客数',
  UNIQUE KEY `uk_date_post` (`stat_date`, `post_id`),
  INDEX `idx_date` (`stat_date`),
  CONSTRAINT `fk_view_stats_post` FOREIGN KEY (`post_id`) REFERENCES `posts`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问统计表';
```

#### `audit_logs` - 审计日志表
```sql
CREATE TABLE `audit_logs` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE',
  `entity_type` VARCHAR(50) NOT NULL COMMENT '实体类型：Post/Comment/Tag',
  `entity_id` BIGINT UNSIGNED NOT NULL COMMENT '实体ID',
  `changes` JSON DEFAULT NULL COMMENT '变更内容（JSON格式）',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user` (`user_id`),
  INDEX `idx_entity` (`entity_type`, `entity_id`),
  INDEX `idx_created` (`created_at`),
  CONSTRAINT `fk_audit_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
```

#### `post_revisions` - 文章修订历史表
```sql
CREATE TABLE `post_revisions` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '修订ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` LONGTEXT NOT NULL COMMENT '正文',
  `revised_by` BIGINT UNSIGNED NOT NULL COMMENT '修订人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_post` (`post_id`),
  INDEX `idx_created` (`created_at`),
  CONSTRAINT `fk_post_revisions_post` FOREIGN KEY (`post_id`) REFERENCES `posts`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_revisions_user` FOREIGN KEY (`revised_by`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章修订历史表';
```

### 7.3 Redis 数据结构设计

#### 7.3.1 缓存数据（String/Hash）
```
# 用户信息缓存
Key: user:info:{userId}
Value: JSON 格式的用户对象
TTL: 30 分钟

# 文章详情缓存
Key: post:detail:{slug}
Value: JSON 格式的文章对象
TTL: 5 分钟（Caffeine L1 缓存后降级到 Redis）

# 文章列表缓存
Key: post:list:{page}:{size}:{tag}:{sort}
Value: JSON 格式的分页结果
TTL: 10 分钟

# 标签列表缓存
Key: tag:list:all
Value: JSON 格式的标签数组
TTL: 10 分钟

# 站点配置缓存
Key: site:settings
Value: Hash 类型，field 为配置键，value 为配置值
TTL: 30 分钟
```

#### 7.3.2 Token 管理（String）
```
# Refresh Token 存储
Key: refresh_token:{userId}:{tokenId}
Value: JSON 格式 {"userId": xxx, "createdAt": xxx}
TTL: 7 天

# Token 黑名单（登出）
Key: token:blacklist:{tokenId}
Value: 1
TTL: Token 剩余有效期
```

#### 7.3.3 计数器（String/Hash）
```
# 文章阅读量（实时累加）
Key: post:view:{postId}
Value: 累计阅读量
说明: 定时任务每 5 分钟刷入 MySQL

# 文章点赞数
Key: post:like_count:{postId}
Value: 点赞数

# 今日全站 PV
Key: pv:daily:{date}
Value: 累计访问量

# 在线用户（活跃心跳）
Key: online:user:{userId}
Value: 最后活跃时间戳
TTL: 5 分钟
```

#### 7.3.4 集合类型（Set/ZSet）
```
# 文章点赞用户集合（判断用户是否已点赞）
Key: post:likes:{postId}
Type: Set
Members: userId 列表

# 热门文章排行榜（按阅读量）
Key: post:ranking:views
Type: ZSet
Score: 阅读量
Member: postId

# 热门标签排行榜（按使用次数）
Key: tag:ranking:usage
Type: ZSet
Score: 使用次数
Member: tagId
```

#### 7.3.5 HyperLogLog（UV 统计）
```
# 今日全站 UV
Key: uv:daily:{date}
Type: HyperLogLog
说明: PFADD 添加访客 IP 或 userId

# 文章 UV（按日）
Key: uv:post:{postId}:{date}
Type: HyperLogLog
```

#### 7.3.6 限流（String/ZSet）
```
# 滑动窗口限流（登录接口）
Key: rate_limit:login:{ip}
Type: ZSet
Score: 时间戳
说明: 限制 5 次/分钟

# Token Bucket 限流（评论接口）
Key: rate_limit:comment:{userId}
Type: String
Value: 剩余令牌数
```

#### 7.3.7 分布式锁（String）
```
# 文章阅读量刷入数据库锁
Key: lock:flush_view_count
Value: 锁持有者标识（UUID）
TTL: 30 秒
```

### 7.4 数据库索引优化

#### 7.4.1 复合索引（按查询频率）
```sql
-- 文章列表查询（首页）
CREATE INDEX idx_posts_status_published ON posts(status, published_at DESC, is_top DESC);

-- 文章列表查询（按作者）
CREATE INDEX idx_posts_author_status ON posts(author_id, status, created_at DESC);

-- 评论查询（按文章）
CREATE INDEX idx_comments_post_status ON comments(post_id, status, created_at ASC);

-- 统计查询（按日期）
CREATE INDEX idx_view_stats_date_post ON view_stats(stat_date DESC, post_id);

-- 审计日志查询
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at DESC);
```

#### 7.4.2 全文索引（MySQL）
```sql
-- 使用 ngram 分词器支持中文搜索
CREATE FULLTEXT INDEX idx_posts_fulltext ON posts(title, content) WITH PARSER ngram;

-- 全文搜索查询示例
SELECT * FROM posts 
WHERE MATCH(title, content) AGAINST('Spring Boot' IN NATURAL LANGUAGE MODE)
AND status = 'published' 
AND deleted_at IS NULL;
```

## 8. 安全与合规设计

- **密码存储**：统一使用 BCrypt 加密（strength=10）
- **跨域设置**：
  - Spring Security 中启用 CORS
  - 允许前端域名访问 API
  - 生产环境严格限制允许的 Origin
- **CSRF 防护**：
  - 因采用 JWT 无状态认证，可禁用 Session-based CSRF
  - 但 Refresh Token 使用 Cookie，需启用 SameSite 属性（SameSite=Strict）
- **请求限流（基于 Redis + Lua 脚本）**：
  - 登录接口：5 次/分钟（单 IP）
  - 注册接口：3 次/小时（单 IP）
  - 评论接口：10 次/小时（单用户）
  - 文章发布：30 次/天（管理员）
  - 使用令牌桶或滑动窗口算法
- **输入校验**：
  - 使用 Hibernate Validator 对请求参数进行校验
  - 统一返回校验错误信息（`@Valid` + `@ControllerAdvice`）
  - 自定义校验注解（如 `@ValidSlug`）
- **XSS/SQL 注入防护**：
  - 使用 JPA/Hibernate 规避 SQL 注入风险
  - 对用户输入内容进行转义（使用 OWASP Java Encoder）
  - 评论内容使用白名单过滤 HTML 标签
  - 前端使用 DOMPurify 库清洗 HTML
- **敏感词过滤**：
  - 集成敏感词库（DFA 算法）
  - 评论、文章标题实时过滤
  - 可配置敏感词处理策略（拒绝/替换/人工审核）
- **图片内容审核**（可选）：
  - 接入阿里云/腾讯云内容安全 API
  - 上传图片自动审核（色情、暴力、违禁）
- **日志与操作审计**：
  - 记录管理员关键操作日志（`audit_logs` 表）
  - 删除文章、审核评论、修改配置等操作记录
  - 记录操作前后的数据变化（JSON 格式）
- **数据备份**：
  - MySQL 定时备份（使用 mysqldump 或 Percona XtraBackup）
  - 备份策略：全量备份（每日）+ 增量备份（每小时）
  - 备份文件加密后上传到云存储（阿里云 OSS/AWS S3）
  - Redis 持久化：启用 AOF + RDB 双重保障
- **HTTPS 强制**：
  - Nginx 配置 SSL 证书（Let's Encrypt）
  - 强制 HTTP 跳转 HTTPS
  - 配置 HSTS 响应头
- **接口签名验证**（可选，开放 API 场景）：
  - 使用 HMAC-SHA256 签名
  - 防止参数篡改和重放攻击

## 9. 部署与运维方案

### 9.1 Docker Compose 一键部署

#### `docker-compose.yml`
```yaml
version: '3.8'

services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: blog_mysql
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: blog_db
      MYSQL_USER: blog_user
      MYSQL_PASSWORD: blog_pass
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    command: --default-authentication-plugin=mysql_native_password --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    networks:
      - blog_network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: blog_redis
    command: redis-server --appendonly yes --requirepass redis_password
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - blog_network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Spring Boot 后端
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: blog_backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/blog_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: blog_user
      SPRING_DATASOURCE_PASSWORD: blog_pass
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_REDIS_PASSWORD: redis_password
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    ports:
      - "8080:8080"
    networks:
      - blog_network
    volumes:
      - ./uploads:/app/uploads
    restart: unless-stopped

  # Vue 前端 + Nginx
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: blog_frontend
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - backend
    networks:
      - blog_network
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    restart: unless-stopped

volumes:
  mysql_data:
  redis_data:

networks:
  blog_network:
    driver: bridge
```

#### 后端 `Dockerfile`
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/blog-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "app.jar"]
```

#### 前端 `Dockerfile`
```dockerfile
# 构建阶段
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 生产阶段
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 9.2 Nginx 配置

#### `nginx.conf`
```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    keepalive_timeout 65;
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # 后端 upstream
    upstream backend_servers {
        server backend:8080;
    }

    # HTTP 重定向到 HTTPS
    server {
        listen 80;
        server_name your-domain.com;
        return 301 https://$host$request_uri;
    }

    # HTTPS 服务器
    server {
        listen 443 ssl http2;
        server_name your-domain.com;

        # SSL 证书配置
        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;

        # HSTS
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

        # 前端静态资源
        location / {
            root /usr/share/nginx/html;
            try_files $uri $uri/ /index.html;
            
            # 缓存策略
            location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
                expires 1y;
                add_header Cache-Control "public, immutable";
            }
        }

        # API 反向代理
        location /api/ {
            proxy_pass http://backend_servers/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            # WebSocket 支持（如需要）
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            
            # 超时设置
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        # 文件上传（直接访问后端存储）
        location /uploads/ {
            alias /var/www/blog/uploads/;
            expires 1y;
            add_header Cache-Control "public";
        }
    }
}
```

### 9.3 应用配置文件

#### `application-prod.yml`
```yaml
spring:
  application:
    name: blog-backend
  
  # 数据源配置
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: none  # 生产环境使用 Flyway 管理
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: false
  
  # Redis 配置
  data:
    redis:
      host: ${SPRING_REDIS_HOST}
      port: ${SPRING_REDIS_PORT}
      password: ${SPRING_REDIS_PASSWORD}
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms
  
  # 缓存配置（使用 Redis）
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 默认 10 分钟
      cache-null-values: false
      key-prefix: "blog:cache:"
      use-key-prefix: true
  
  # Flyway 数据库迁移
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  
  # 文件上传
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

# JWT 配置
jwt:
  secret: your_secret_key_at_least_256_bits_long
  access-token-expiration: 900000  # 15 分钟（毫秒）
  refresh-token-expiration: 604800000  # 7 天（毫秒）

# Actuator 监控
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# 日志配置
logging:
  level:
    root: INFO
    com.example.blog: DEBUG
  file:
    name: /var/log/blog/application.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 9.4 部署流程

1. **前端打包**：
   ```bash
   cd frontend
   npm install
   npm run build
   # 生成 dist/ 目录
   ```

2. **后端打包**：
   ```bash
   cd backend
   mvn clean package -DskipTests
   # 生成 target/blog-backend.jar
   ```

3. **Docker Compose 启动**：
   ```bash
   docker-compose up -d
   ```

4. **查看日志**：
   ```bash
   docker-compose logs -f backend
   ```

5. **数据库初始化**：
   - Flyway 自动执行迁移脚本（`src/main/resources/db/migration`）

### 9.5 监控与日志

#### Prometheus 指标采集
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'blog-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

#### Grafana 仪表盘
- JVM 监控（堆内存、GC、线程数）
- MySQL 监控（慢查询、连接数）
- Redis 监控（命中率、内存使用）
- 业务指标（注册数、发文量、QPS）

#### 日志收集（可选）
- 使用 ELK Stack 或 Loki + Grafana
- 应用日志输出为 JSON 格式
- Nginx Access Log 接入日志系统

### 9.6 CI/CD（GitHub Actions 示例）

#### `.github/workflows/deploy.yml`
```yaml
name: Deploy Blog System

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build Backend
      run: |
        cd backend
        mvn clean package -DskipTests
    
    - name: Build Frontend
      run: |
        cd frontend
        npm install
        npm run build
    
    - name: Deploy to Server
      uses: appleboy/scp-action@master
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SSH_PRIVATE_KEY }}
        source: "backend/target/*.jar,frontend/dist/*,docker-compose.yml"
        target: "/opt/blog"
    
    - name: Restart Services
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SSH_PRIVATE_KEY }}
        script: |
          cd /opt/blog
          docker-compose down
          docker-compose up -d --build
```

## 10. 开发迭代计划

### 10.1 第一阶段（MVP，约 2–3 周）
- 实现基础功能：
  - 用户注册/登录
  - 文章发布/编辑/删除（管理员）
  - 文章列表与详情页
  - 评论功能（无审核或简单审核）
  - 基础标签管理
- 目标：可用的个人博客系统

### 10.2 第二阶段（增强版）
- 点赞、收藏功能
- 仪表盘统计页面
- 完善标签、分类功能
- 基础防刷限流

### 10.3 第三阶段（优化）
- 全文搜索（FTS5）
- 富文本编辑体验优化
- 数据备份/恢复机制
- 第三方登录集成

### 10.4 长期规划
- SSR 或静态页面预渲染，提高 SEO 能力
- 移动端体验优化（PWA）
- 引入推荐算法，基于用户行为推荐文章
- AI 辅助写作/摘要生成（后续可接入大模型 API）