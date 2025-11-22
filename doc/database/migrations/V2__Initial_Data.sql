-- Flyway Migration V2: 初始化数据
-- 版本：1.0.0
-- 描述：插入系统初始数据（角色、配置、管理员账户等）

-- 初始化角色
INSERT INTO `roles` (`name`, `description`) VALUES 
('ROLE_ADMIN', '管理员'),
('ROLE_USER', '普通用户');

-- 初始化站点配置
INSERT INTO `site_settings` (`setting_key`, `setting_value`, `description`) VALUES
('site_title', '我的博客', '站点标题'),
('site_subtitle', '分享技术与生活', '站点副标题'),
('site_description', '这是一个基于Vue3和Spring Boot构建的博客系统', '站点描述'),
('site_keywords', 'Vue3,Spring Boot,博客,技术分享', 'SEO关键词'),
('comment_need_review', '1', '评论是否需要审核：1是，0否'),
('allow_anonymous_comment', '0', '是否允许匿名评论：1是，0否'),
('posts_per_page', '10', '每页文章数量'),
('enable_registration', '1', '是否开放注册：1是，0否');

-- 创建默认管理员账户
-- 用户名：admin
-- 密码：admin123（BCrypt 加密后）
INSERT INTO `users` (`username`, `email`, `password`, `nickname`, `status`) VALUES
('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 1);

-- 为管理员分配角色
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(1, 1),  -- ROLE_ADMIN
(1, 2);  -- ROLE_USER

-- 创建默认分类
INSERT INTO `categories` (`name`, `slug`, `description`, `sort_order`) VALUES
('技术分享', 'tech', '技术相关文章', 1),
('生活随笔', 'life', '生活感悟与随笔', 2),
('学习笔记', 'study', '学习过程中的笔记', 3);

-- 创建默认标签
INSERT INTO `tags` (`name`, `description`) VALUES
('Java', 'Java 编程语言'),
('Spring Boot', 'Spring Boot 框架'),
('Vue', 'Vue.js 前端框架'),
('MySQL', 'MySQL 数据库'),
('Redis', 'Redis 缓存'),
('Docker', 'Docker 容器技术'),
('微服务', '微服务架构'),
('前端开发', '前端开发相关'),
('后端开发', '后端开发相关'),
('数据库', '数据库相关技术');
