# 故障预案手册

## 目录
- [数据库故障处理](#数据库故障处理)
- [缓存故障处理](#缓存故障处理)
- [应用故障处理](#应用故障处理)
- [网络攻击防护](#网络攻击防护)
- [应急响应流程](#应急响应流程)

---

## 数据库故障处理

### 1. MySQL 宕机

#### 故障现象
- 应用无法连接数据库
- 日志出现 "Communications link failure"
- 健康检查失败

#### 应急处理流程

**步骤1: 快速诊断**
```bash
# 检查 MySQL 进程
docker ps | grep mysql
systemctl status mysql

# 检查日志
docker logs blog-mysql --tail=100

# 检查磁盘空间
df -h
```

**步骤2: 重启 MySQL**
```bash
docker restart blog-mysql
sleep 30
docker exec blog-mysql mysqladmin ping -h localhost
```

**步骤3: 数据检查**
```sql
CHECK TABLE posts;
REPAIR TABLE posts;
SELECT COUNT(*) FROM posts;
```

#### 预防措施

**自动备份脚本**
```bash
#!/bin/bash
BACKUP_DIR=/backup/mysql
DATE=$(date +%Y%m%d_%H%M%S)

mysqldump -u root -p$MYSQL_ROOT_PASSWORD \
  --single-transaction blog_db | gzip > $BACKUP_DIR/backup_$DATE.sql.gz

find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
```

### 2. 连接池耗尽

**配置优化**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
      leak-detection-threshold: 60000
```

---

## 缓存故障处理

### 1. Redis 宕机

#### 降级策略
```java
@Service
public class PostService {
    public PostDTO getPost(String slug) {
        try {
            return getFromRedis(slug);
        } catch (Exception e) {
            log.error("Redis error, fallback to DB", e);
            return getFromDatabase(slug);
        }
    }
}
```

### 2. 缓存击穿

**分布式锁方案**
```java
public PostDTO getPost(String slug) {
    RLock lock = redissonClient.getLock("lock:post:" + slug);
    try {
        if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
            return loadAndCache(slug);
        }
    } finally {
        lock.unlock();
    }
    return loadFromDatabase(slug);
}
```

### 3. 缓存雪崩

**随机过期时间**
```java
public void cachePost(PostDTO post) {
    long baseExpire = 5 * 60;
    long randomExpire = ThreadLocalRandom.current().nextLong(60);
    redisTemplate.opsForValue().set(
        "post:" + post.getSlug(), 
        post, 
        baseExpire + randomExpire, 
        TimeUnit.SECONDS
    );
}
```

---

## 应用故障处理

### 1. 内存溢出 (OOM)

```bash
# 生成堆转储
docker exec blog-backend jmap -dump:live,format=b,file=/tmp/heap.hprof <PID>

# 导出分析
docker cp blog-backend:/tmp/heap.hprof ./
```

**JVM 优化**
```dockerfile
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
```

### 2. CPU 100%

```bash
# 查看线程
docker exec blog-backend top -H -p <PID>

# 线程转储
docker exec blog-backend jstack <PID> > thread_dump.txt
```

### 3. 接口超时

**限流配置**
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) {
        String ip = getClientIP(request);
        String key = "rate_limit:" + ip;
        
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        
        if (count > 100) {
            response.setStatus(429);
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 网络攻击防护

### 1. DDoS 防护

**Nginx 限流**
```nginx
http {
    limit_req_zone $binary_remote_addr zone=req_limit:10m rate=10r/s;
    limit_req zone=req_limit burst=20 nodelay;
    
    limit_conn_zone $binary_remote_addr zone=conn_limit:10m;
    limit_conn conn_limit 10;
}
```

### 2. SQL 注入防护

```java
// 使用参数化查询
@Query("SELECT p FROM Post p WHERE p.slug = :slug")
Optional<Post> findBySlug(@Param("slug") String slug);
```

### 3. XSS 防护

```java
@PostMapping
public ResponseEntity<CommentDTO> createComment(@RequestBody @Valid CommentRequest request) {
    String sanitizedContent = HtmlUtils.htmlEscape(request.getContent());
    return ResponseEntity.ok(commentService.save(sanitizedContent));
}
```

---

## 应急响应流程

### 1. 故障分级

| 级别 | 描述 | 响应时间 | 处理人员 |
|-----|------|---------|---------|
| P0 | 服务完全不可用 | 5分钟 | 全员 |
| P1 | 核心功能不可用 | 15分钟 | 技术负责人 + 相关开发 |
| P2 | 部分功能异常 | 1小时 | 相关开发 |
| P3 | 性能下降 | 4小时 | 值班开发 |

### 2. 应急流程

```
1. 故障发现（监控告警/用户反馈）
   ↓
2. 快速评估（确定故障级别）
   ↓
3. 组建应急小组（根据级别）
   ↓
4. 故障定位（查看日志、监控）
   ↓
5. 应急处理（回滚/降级/重启）
   ↓
6. 服务恢复（验证功能）
   ↓
7. 复盘总结（根因分析、改进措施）
```

### 3. 故障报告模板

```markdown
# 故障报告

## 基本信息
- 故障时间: 2024-01-01 10:00:00
- 故障级别: P1
- 影响范围: 全部用户无法访问
- 恢复时间: 2024-01-01 10:30:00
- 持续时长: 30 分钟

## 故障现象
- 用户无法访问首页
- API 返回 500 错误

## 根本原因
- MySQL 主库磁盘空间满

## 处理过程
1. 10:05 - 收到监控告警
2. 10:10 - 确认数据库无法连接
3. 10:15 - 清理日志文件
4. 10:20 - 重启 MySQL
5. 10:30 - 服务恢复

## 改进措施
1. 增加磁盘空间监控
2. 配置日志自动清理
3. 优化慢查询日志
```

---

## 总结

本故障预案提供了：

1. ✅ 数据库故障应急方案
2. ✅ 缓存故障处理流程
3. ✅ 应用故障诊断方法
4. ✅ 网络攻击防护策略
5. ✅ 标准应急响应流程

**关键原则**:
- 快速响应，及时止损
- 先恢复服务，再追查原因
- 做好监控告警，提前预防
- 定期演练，熟悉流程
