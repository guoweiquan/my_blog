# 数据库迁移脚本指南

## 目录
- [Flyway 配置](#flyway-配置)
- [版本化管理](#版本化管理)
- [迁移脚本](#迁移脚本)
- [回滚脚本](#回滚脚本)
- [最佳实践](#最佳实践)

---

## Flyway 配置

### 1. Maven 依赖

```xml
<dependencies>
    <!-- Flyway Core -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
        <version>9.22.0</version>
    </dependency>
    
    <!-- Flyway MySQL -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
        <version>9.22.0</version>
    </dependency>
</dependencies>
```

### 2. application.yml 配置

```yaml
spring:
  flyway:
    enabled: true
    # 迁移脚本位置
    locations: classpath:db/migration
    # 基线版本
    baseline-on-migrate: true
    baseline-version: 0
    # 校验
    validate-on-migrate: true
    # 占位符
    placeholder-replacement: true
    placeholders:
      db.name: blog_db
      db.charset: utf8mb4
    # 编码
    encoding: UTF-8
    # 清理数据库（生产环境必须为 false）
    clean-disabled: true
    # 表名
    table: flyway_schema_history
    
  datasource:
    url: jdbc:mysql://localhost:3306/blog_db?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: blog_user
    password: ${DB_PASSWORD:blog_password}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 3. 多环境配置

```yaml
---
# 开发环境
spring:
  config:
    activate:
      on-profile: dev
  flyway:
    clean-disabled: false  # 开发环境允许清理
    baseline-on-migrate: true

---
# 生产环境
spring:
  config:
    activate:
      on-profile: prod
  flyway:
    clean-disabled: true   # 生产环境禁止清理
    out-of-order: false    # 禁止乱序执行
    validate-on-migrate: true
```

---

## 版本化管理

### 1. 命名规范

```
格式: V{version}__{description}.sql
示例:
  V1__Initial_Schema.sql           # 初始化表结构
  V2__Initial_Data.sql             # 初始化数据
  V3__Add_Post_Views_Column.sql    # 添加浏览量字段
  V4__Create_Index_On_Posts.sql    # 创建索引
  V5__Alter_Comment_Table.sql      # 修改评论表

回滚脚本格式: U{version}__{description}.sql
示例:
  U3__Rollback_Post_Views_Column.sql
  U4__Rollback_Index_On_Posts.sql
```

### 2. 版本号规则

```
主版本.次版本.修订号
V1.0.0  - 初始版本
V1.1.0  - 添加新功能
V1.1.1  - Bug 修复
V2.0.0  - 重大变更

或使用时间戳:
V20231201120000__Add_User_Email.sql  # 2023-12-01 12:00:00
```

### 3. 目录结构

```
src/main/resources/
└── db/
    └── migration/
        ├── V1__Initial_Schema.sql
        ├── V2__Initial_Data.sql
        ├── V3__Add_Post_Views_Column.sql
        ├── V4__Create_Index_On_Posts.sql
        └── rollback/
            ├── U3__Rollback_Post_Views_Column.sql
            └── U4__Rollback_Index_On_Posts.sql
```

---

## 迁移脚本

### V1__Initial_Schema.sql

```sql
-- =============================================
-- 版本: V1
-- 描述: 初始化数据库表结构
-- 日期: 2024-01-01
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    bio TEXT COMMENT '个人简介',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, BANNED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at DATETIME COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色代码',
    description VARCHAR(200) COMMENT '角色描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称',
    slug VARCHAR(50) NOT NULL UNIQUE COMMENT '分类别名',
    description VARCHAR(200) COMMENT '分类描述',
    parent_id BIGINT COMMENT '父分类ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    post_count INT DEFAULT 0 COMMENT '文章数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    slug VARCHAR(50) NOT NULL UNIQUE COMMENT '标签别名',
    post_count INT DEFAULT 0 COMMENT '文章数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_count (post_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 文章表
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    slug VARCHAR(200) NOT NULL UNIQUE COMMENT '别名',
    summary VARCHAR(500) COMMENT '摘要',
    content LONGTEXT NOT NULL COMMENT '内容（Markdown）',
    content_html LONGTEXT COMMENT '内容（HTML）',
    cover_image VARCHAR(500) COMMENT '封面图',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    category_id BIGINT COMMENT '分类ID',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT, PUBLISHED, ARCHIVED',
    is_top BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    is_featured BOOLEAN DEFAULT FALSE COMMENT '是否精选',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    published_at DATETIME COMMENT '发布时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_slug (slug),
    INDEX idx_author_id (author_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at DESC),
    INDEX idx_is_top_published (is_top, published_at DESC),
    FULLTEXT idx_fulltext_search (title, summary, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS post_tags (
    post_id BIGINT NOT NULL COMMENT '文章ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- 评论表
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    post_id BIGINT NOT NULL COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    parent_id BIGINT COMMENT '父评论ID',
    content TEXT NOT NULL COMMENT '评论内容',
    status VARCHAR(20) DEFAULT 'APPROVED' COMMENT '状态: PENDING, APPROVED, REJECTED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 点赞表
CREATE TABLE IF NOT EXISTS likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    post_id BIGINT NOT NULL COMMENT '文章ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_post (user_id, post_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 收藏表
CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    post_id BIGINT NOT NULL COMMENT '文章ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_post (user_id, post_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 站点设置表
CREATE TABLE IF NOT EXISTS site_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设置ID',
    setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '设置键',
    setting_value TEXT COMMENT '设置值',
    description VARCHAR(200) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点设置表';
```

### V2__Initial_Data.sql

```sql
-- =============================================
-- 版本: V2
-- 描述: 初始化基础数据
-- 日期: 2024-01-01
-- =============================================

-- 插入角色数据
INSERT INTO roles (name, code, description) VALUES
('管理员', 'ADMIN', '系统管理员，拥有所有权限'),
('作者', 'AUTHOR', '可以发布文章'),
('用户', 'USER', '普通用户');

-- 插入站点设置
INSERT INTO site_settings (setting_key, setting_value, description) VALUES
('site.name', '我的博客', '网站名称'),
('site.description', '记录生活，分享技术', '网站描述'),
('site.keywords', 'blog,技术,生活', '网站关键词'),
('site.icp', '', 'ICP备案号');

-- 创建默认管理员账户（密码: admin123）
INSERT INTO users (username, email, password, nickname, status) VALUES
('admin', 'admin@blog.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'ACTIVE');

-- 为管理员分配角色
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.username = 'admin' AND r.code = 'ADMIN';

-- 创建默认分类
INSERT INTO categories (name, slug, description, sort_order) VALUES
('技术', 'tech', '技术相关文章', 1),
('生活', 'life', '生活随笔', 2),
('其他', 'other', '其他内容', 3);
```

### V3__Add_Post_Views_Column.sql

```sql
-- =============================================
-- 版本: V3
-- 描述: 为文章表添加浏览量统计相关字段
-- 日期: 2024-01-15
-- =============================================

-- 添加今日浏览量字段
ALTER TABLE posts 
ADD COLUMN view_count_today INT DEFAULT 0 COMMENT '今日浏览量' AFTER view_count;

-- 添加本周浏览量字段
ALTER TABLE posts 
ADD COLUMN view_count_week INT DEFAULT 0 COMMENT '本周浏览量' AFTER view_count_today;

-- 添加本月浏览量字段
ALTER TABLE posts 
ADD COLUMN view_count_month INT DEFAULT 0 COMMENT '本月浏览量' AFTER view_count_week;

-- 创建浏览统计表
CREATE TABLE IF NOT EXISTS view_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    post_id BIGINT NOT NULL COMMENT '文章ID',
    view_date DATE NOT NULL COMMENT '日期',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    unique_visitors INT DEFAULT 0 COMMENT '独立访客数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_post_date (post_id, view_date),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    INDEX idx_view_date (view_date DESC),
    INDEX idx_view_count (view_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览统计表';
```

### V4__Create_Index_On_Posts.sql

```sql
-- =============================================
-- 版本: V4
-- 描述: 优化文章表索引
-- 日期: 2024-02-01
-- =============================================

-- 创建组合索引：状态 + 发布时间
CREATE INDEX idx_status_published_at ON posts(status, published_at DESC);

-- 创建组合索引：作者 + 状态
CREATE INDEX idx_author_status ON posts(author_id, status);

-- 创建组合索引：分类 + 状态 + 发布时间
CREATE INDEX idx_category_status_published ON posts(category_id, status, published_at DESC);

-- 分析表以优化查询
ANALYZE TABLE posts;
```

### V5__Add_Audit_Log_Table.sql

```sql
-- =============================================
-- 版本: V5
-- 描述: 添加审计日志表
-- 日期: 2024-02-15
-- =============================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(50) NOT NULL COMMENT '操作类型: CREATE, UPDATE, DELETE',
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型: POST, COMMENT, USER',
    entity_id BIGINT COMMENT '实体ID',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent TEXT COMMENT 'User Agent',
    request_url VARCHAR(500) COMMENT '请求URL',
    old_value TEXT COMMENT '旧值（JSON）',
    new_value TEXT COMMENT '新值（JSON）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_operation (operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
```

---

## 回滚脚本

### U3__Rollback_Post_Views_Column.sql

```sql
-- =============================================
-- 回滚版本: V3
-- 描述: 回滚文章浏览量统计字段
-- 日期: 2024-01-15
-- =============================================

-- 删除浏览统计表
DROP TABLE IF EXISTS view_stats;

-- 删除添加的字段
ALTER TABLE posts DROP COLUMN IF EXISTS view_count_month;
ALTER TABLE posts DROP COLUMN IF EXISTS view_count_week;
ALTER TABLE posts DROP COLUMN IF EXISTS view_count_today;
```

### U4__Rollback_Index_On_Posts.sql

```sql
-- =============================================
-- 回滚版本: V4
-- 描述: 回滚文章表索引优化
-- 日期: 2024-02-01
-- =============================================

-- 删除添加的索引
DROP INDEX IF EXISTS idx_category_status_published ON posts;
DROP INDEX IF EXISTS idx_author_status ON posts;
DROP INDEX IF EXISTS idx_status_published_at ON posts;
```

### U5__Rollback_Audit_Log_Table.sql

```sql
-- =============================================
-- 回滚版本: V5
-- 描述: 回滚审计日志表
-- 日期: 2024-02-15
-- =============================================

-- 删除审计日志表
DROP TABLE IF EXISTS audit_logs;
```

---

## 最佳实践

### 1. 脚本编写规范

```sql
-- ✅ 好的实践
-- 使用 IF EXISTS / IF NOT EXISTS
DROP TABLE IF EXISTS temp_table;
CREATE TABLE IF NOT EXISTS new_table (...);

-- 添加注释
ALTER TABLE posts 
ADD COLUMN new_field VARCHAR(100) COMMENT '新字段说明';

-- 使用事务（非 DDL 语句）
START TRANSACTION;
-- DML 操作
UPDATE posts SET status = 'PUBLISHED' WHERE status = 'DRAFT';
COMMIT;

-- ❌ 避免的做法
-- 不要直接删除字段（应该先备份）
ALTER TABLE posts DROP COLUMN important_field;

-- 不要使用 SELECT * 
SELECT * FROM posts;  -- 应该明确列出字段

-- 不要在生产环境使用 TRUNCATE
TRUNCATE TABLE posts;
```

### 2. 回滚策略

```bash
# 方式1: 手动执行回滚脚本
mysql -u blog_user -p blog_db < rollback/U5__Rollback_Audit_Log_Table.sql

# 方式2: 使用 Flyway Undo（企业版功能）
flyway undo

# 方式3: 恢复数据库备份
mysql -u blog_user -p blog_db < backup_20240215.sql
```

### 3. 数据备份

```bash
#!/bin/bash
# backup.sh - 数据库备份脚本

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/mysql"
DB_NAME="blog_db"
DB_USER="blog_user"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 全量备份
mysqldump -u $DB_USER -p$DB_PASSWORD \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  $DB_NAME > $BACKUP_DIR/backup_$DATE.sql

# 压缩备份文件
gzip $BACKUP_DIR/backup_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete

echo "Backup completed: backup_$DATE.sql.gz"
```

### 4. 迁移测试流程

```bash
# 1. 在开发环境测试
mvn flyway:migrate -Dflyway.configFiles=flyway-dev.conf

# 2. 在测试环境验证
mvn flyway:migrate -Dflyway.configFiles=flyway-test.conf

# 3. 检查迁移状态
mvn flyway:info

# 4. 验证数据完整性
mysql -u blog_user -p -e "SELECT COUNT(*) FROM posts"

# 5. 生产环境迁移（先备份）
./backup.sh
mvn flyway:migrate -Dflyway.configFiles=flyway-prod.conf
```

### 5. 常用命令

```bash
# 查看迁移状态
mvn flyway:info

# 执行迁移
mvn flyway:migrate

# 验证迁移
mvn flyway:validate

# 修复元数据（慎用）
mvn flyway:repair

# 清理数据库（仅开发环境）
mvn flyway:clean

# 基线初始化
mvn flyway:baseline
```

### 6. 灰度发布策略

```java
@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            // 配置回调
            .callbacks(new FlywayCallback())
            .load();
            
        // 仅在主节点执行迁移
        if (isLeaderNode()) {
            flyway.migrate();
        }
        
        return flyway;
    }
    
    private boolean isLeaderNode() {
        // 通过 Redis 分布式锁或配置中心判断
        return true;
    }
}

class FlywayCallback implements Callback {
    @Override
    public void handle(Event event, Context context) {
        if (event == Event.AFTER_MIGRATE) {
            // 迁移成功后的操作
            log.info("Migration completed successfully");
            // 发送通知、记录审计日志等
        }
    }
}
```

---

## 故障处理

### 问题1: 迁移失败

```sql
-- 查看 Flyway 元数据表
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

-- 修复失败的迁移
UPDATE flyway_schema_history SET success = 1 WHERE version = '5';

-- 或使用 repair 命令
mvn flyway:repair
```

### 问题2: 版本冲突

```bash
# 场景：多人同时提交了 V5 版本的脚本

# 解决方案：
# 1. 重命名后提交的脚本为 V6
# 2. 或使用时间戳版本号避免冲突
V20240215120000__Add_Field.sql
```

### 问题3: 生产环境回滚

```bash
# 1. 立即停止应用
systemctl stop blog-app

# 2. 恢复数据库备份
mysql -u blog_user -p blog_db < backup_before_migration.sql

# 3. 回滚代码版本
git checkout <previous-tag>
mvn clean package -DskipTests

# 4. 重启应用
systemctl start blog-app

# 5. 验证服务
curl http://localhost:8080/actuator/health
```

---

## 总结

通过 Flyway 版本化管理数据库变更，我们可以：

1. ✅ 自动化数据库迁移
2. ✅ 版本控制和追溯
3. ✅ 支持多环境部署
4. ✅ 提供回滚机制
5. ✅ 团队协作更规范

**关键点**:
- 迁移脚本必须幂等
- 生产环境必须先备份
- 使用命名规范和版本号
- 充分测试后再上线
