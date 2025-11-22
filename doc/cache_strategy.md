# 缓存策略指南（Redis 单层缓存）

## 1. 缓存架构概览

博客系统采用 **Redis 单层缓存**架构，简单高效：

```
用户请求 → Spring Boot 应用 → Redis 缓存
                              ↓ (未命中)
                           MySQL 数据库
```

**架构优势**：
- ✅ 架构简单，易于理解和维护
- ✅ 减少 80% 数据库查询压力
- ✅ 支持分布式部署（多实例共享缓存）
- ✅ 成本可控，适合中小规模应用

---

## 2. 缓存数据分类

### 2.1 读缓存（查询加速）

| 数据类型 | 缓存 Key | TTL | 更新策略 |
|---------|---------|-----|---------|
| 文章详情 | `post:detail:{slug}` | 5 分钟 | 文章更新时删除 |
| 文章列表 | `post:list:{page}:{size}:{tag}:{sort}` | 10 分钟 | 文章增删改时清空 |
| 用户信息 | `user:info:{userId}` | 30 分钟 | 用户信息更新时删除 |
| 标签列表 | `tag:list:all` | 10 分钟 | 标签变更时删除 |
| 站点配置 | `site:settings` (Hash) | 30 分钟 | 配置更新时删除 |

### 2.2 写缓存（性能优化）

| 数据类型 | 缓存 Key | 说明 | 刷库策略 |
|---------|---------|------|---------|
| 文章阅读量 | `post:view:{postId}` | 实时累加 | 每 5 分钟批量刷入 MySQL |
| 文章点赞数 | `post:like_count:{postId}` | 实时更新 | 定时同步 |
| 今日 PV | `pv:daily:{date}` | 每次访问 +1 | 每日凌晨聚合 |

### 2.3 业务缓存（功能支持）

| 数据类型 | 缓存 Key | 数据结构 | 说明 |
|---------|---------|---------|------|
| 点赞用户列表 | `post:likes:{postId}` | Set | 判断是否已点赞 |
| 热门文章排行 | `post:ranking:views` | ZSet | TOP 10 排行榜 |
| UV 统计 | `uv:daily:{date}` | HyperLogLog | 独立访客统计 |
| Refresh Token | `refresh_token:{userId}:{tokenId}` | String | 7 天有效期 |
| Token 黑名单 | `token:blacklist:{tokenId}` | String | 防止已登出 Token 使用 |

---

## 3. 缓存使用方式

### 3.1 Spring Cache 注解（推荐，简单场景）

**优点**：代码简洁，Spring 自动管理缓存

```java
@Service
@CacheConfig(cacheNames = "postDetail")
public class PostService {
    
    // 查询时自动缓存
    @Cacheable(key = "#slug")
    public PostDTO getPostBySlug(String slug) {
        return postRepository.findBySlug(slug);
    }
    
    // 更新时自动删除缓存
    @CacheEvict(key = "#post.slug")
    public void updatePost(PostDTO post) {
        postRepository.save(post);
    }
    
    // 删除多个缓存
    @Caching(evict = {
        @CacheEvict(key = "#slug"),
        @CacheEvict(cacheNames = "postList", allEntries = true)
    })
    public void deletePost(String slug) {
        postRepository.deleteBySlug(slug);
    }
}
```

### 3.2 RedisTemplate 手动操作（推荐，复杂场景）

**优点**：灵活控制，支持复杂数据结构

```java
@Service
public class PostService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public PostDTO getPost(String slug) {
        String key = "post:detail:" + slug;
        
        // 1. 查缓存
        PostDTO cached = (PostDTO) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        
        // 2. 查数据库
        PostDTO post = postRepository.findBySlug(slug);
        
        // 3. 写缓存
        redisTemplate.opsForValue().set(key, post, 5, TimeUnit.MINUTES);
        
        return post;
    }
    
    // 增加阅读量
    public void incrementView(Long postId) {
        redisTemplate.opsForValue().increment("post:view:" + postId);
    }
    
    // 点赞功能
    public boolean toggleLike(Long postId, Long userId) {
        String key = "post:likes:" + postId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
        
        if (Boolean.TRUE.equals(isMember)) {
            // 取消点赞
            redisTemplate.opsForSet().remove(key, userId);
            redisTemplate.opsForValue().decrement("post:like_count:" + postId);
            return false;
        } else {
            // 点赞
            redisTemplate.opsForSet().add(key, userId);
            redisTemplate.opsForValue().increment("post:like_count:" + postId);
            return true;
        }
    }
}
```

---

## 4. 缓存更新策略

### 4.1 Cache-Aside（旁路缓存）- 主要策略

**适用**：大部分读操作（文章详情、用户信息等）

**流程**：
1. 读取：先查 Redis → 未命中查 MySQL → 写入 Redis
2. 更新：先更新 MySQL → **删除 Redis 缓存**（不是更新！）

**为什么删除而不是更新？**
- 避免并发更新导致脏数据
- 减少不必要的缓存写入
- 下次读取时自动重建最新数据

**代码示例**：
```java
// 读
public PostDTO getPost(Long id) {
    String key = "post:detail:" + id;
    PostDTO cache = redis.get(key);
    if (cache != null) return cache;
    
    PostDTO db = mysql.query(id);
    redis.set(key, db, 5 * 60);  // 5分钟
    return db;
}

// 写（删除缓存）
public void updatePost(PostDTO post) {
    mysql.update(post);
    redis.delete("post:detail:" + post.getId());  // 删除缓存
}
```

### 4.2 Write-Through（写穿）

**适用**：强一致性场景（站点配置）

**流程**：同时更新 MySQL 和 Redis

```java
public void updateSettings(String key, String value) {
    mysql.updateSetting(key, value);
    redis.hset("site:settings", key, value);  // 同步更新
}
```

### 4.3 Write-Behind（异步写回）

**适用**：高频写入场景（阅读量、点赞数）

**流程**：仅更新 Redis，定时任务批量刷入 MySQL

```java
// 实时写 Redis
public void incrementView(Long postId) {
    redis.incr("post:view:" + postId);
}

// 定时任务刷库
@Scheduled(fixedRate = 300000)  // 5分钟
public void flushViewCount() {
    Set<String> keys = redis.keys("post:view:*");
    for (String key : keys) {
        Long postId = extractPostId(key);
        Integer count = redis.get(key);
        mysql.incrementViewCount(postId, count);
        redis.delete(key);  // 刷入后删除
    }
}
```

---

## 5. 缓存失效策略

### 5.1 单个缓存失效

```java
// 文章更新
public void updatePost(Post post) {
    postRepository.save(post);
    
    // 失效文章详情缓存
    redisTemplate.delete("post:detail:" + post.getSlug());
    
    // 失效文章列表缓存（通配符删除）
    Set<String> keys = redisTemplate.keys("post:list:*");
    if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
    }
}
```

### 5.2 批量缓存失效

```java
// 标签更新（影响多篇文章）
public void updateTag(Tag tag) {
    tagRepository.save(tag);
    
    // 清空标签列表缓存
    redisTemplate.delete("tag:list:all");
    
    // 清空所有文章列表缓存
    cacheManager.getCache("postList").clear();
}
```

### 5.3 定时失效（TTL）

所有缓存都设置 TTL，避免永久缓存：
- 热数据：5-10 分钟
- 温数据：30 分钟
- 冷数据：1-2 小时
- 配置数据：30 分钟

---

## 6. 缓存常见问题及解决方案

### 6.1 缓存穿透（查询不存在的数据）

**问题**：恶意查询不存在的文章 ID，每次都打到数据库

**解决方案 1：缓存空值**
```java
public PostDTO getPost(Long id) {
    PostDTO cache = redis.get("post:" + id);
    if (cache != null) {
        if (cache.isEmpty()) {  // 空对象
            throw new NotFoundException();
        }
        return cache;
    }
    
    PostDTO db = mysql.query(id);
    if (db == null) {
        // 缓存空对象，TTL 设置较短（1分钟）
        redis.set("post:" + id, new PostDTO(), 60);
        throw new NotFoundException();
    }
    
    redis.set("post:" + id, db, 300);
    return db;
}
```

**解决方案 2：布隆过滤器**
```java
// 应用启动时加载所有文章 ID 到布隆过滤器
@PostConstruct
public void initBloomFilter() {
    List<Long> postIds = postRepository.findAllIds();
    for (Long id : postIds) {
        bloomFilter.add(id);
    }
}

public PostDTO getPost(Long id) {
    // 先检查布隆过滤器
    if (!bloomFilter.mightContain(id)) {
        throw new NotFoundException("文章不存在");
    }
    // 继续正常查询流程
}
```

### 6.2 缓存击穿（热点数据失效）

**问题**：热门文章缓存过期瞬间，大量请求打到数据库

**解决方案：分布式锁**
```java
public PostDTO getPost(Long id) {
    String key = "post:detail:" + id;
    String lockKey = "lock:post:" + id;
    
    // 1. 查缓存
    PostDTO cache = redis.get(key);
    if (cache != null) return cache;
    
    // 2. 获取锁
    RLock lock = redisson.getLock(lockKey);
    try {
        if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
            // 双重检查
            cache = redis.get(key);
            if (cache != null) return cache;
            
            // 查数据库
            PostDTO db = mysql.query(id);
            redis.set(key, db, 300);
            return db;
        }
    } finally {
        lock.unlock();
    }
}
```

### 6.3 缓存雪崩（大量缓存同时失效）

**问题**：缓存同时过期，数据库瞬间压力过大

**解决方案 1：随机 TTL**
```java
// 不要所有缓存都是 300 秒
int ttl = 300 + new Random().nextInt(60);  // 300-360秒随机
redis.set(key, value, ttl);
```

**解决方案 2：永不过期（后台更新）**
```java
// 设置一个很长的 TTL（如 1 天）
redis.set(key, value, 86400);

// 后台定时任务主动更新热门数据
@Scheduled(fixedRate = 600000)  // 10分钟
public void refreshHotPosts() {
    List<Post> hotPosts = getTop100Posts();
    for (Post post : hotPosts) {
        redis.set("post:detail:" + post.getId(), post, 86400);
    }
}
```

---

## 7. 性能优化建议

### 7.1 批量操作

❌ **不推荐**（N 次网络请求）：
```java
for (Long id : postIds) {
    PostDTO post = redis.get("post:" + id);
}
```

✅ **推荐**（1 次网络请求）：
```java
List<String> keys = postIds.stream()
    .map(id -> "post:" + id)
    .collect(Collectors.toList());
List<PostDTO> posts = redis.opsForValue().multiGet(keys);
```

### 7.2 Pipeline 批量写入

```java
redisTemplate.executePipelined(new SessionCallback<Object>() {
    @Override
    public Object execute(RedisOperations operations) {
        for (Post post : posts) {
            operations.opsForValue().set("post:" + post.getId(), post);
        }
        return null;
    }
});
```

### 7.3 避免大 Key

- 单个 String 类型 < 10KB
- List/Set 元素数 < 5000
- Hash 字段数 < 1000

### 7.4 设置合理的连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20   # 最大连接数
          max-idle: 10     # 最大空闲连接
          min-idle: 5      # 最小空闲连接
          max-wait: 2000ms # 最大等待时间
```

---

## 8. 监控与运维

### 8.1 关键指标

```java
// 查看缓存命中率
INFO stats
// keyspace_hits / (keyspace_hits + keyspace_misses)
// 建议 > 80%

// 查看内存使用
INFO memory
// used_memory / maxmemory 建议 < 80%

// 慢查询
SLOWLOG GET 10
```

### 8.2 告警规则

- 缓存命中率 < 70% → 警告
- 内存使用率 > 80% → 告警
- 慢查询增多 → 优化
- 连接数接近上限 → 扩容

---

## 9. 总结

**Redis 单层缓存架构适用于**：
- ✅ 中小规模博客系统（日活 < 10 万）
- ✅ 读多写少的场景
- ✅ 对一致性要求不是极度严格的场景

**关键原则**：
1. 所有缓存必须设置 TTL
2. 更新数据库后删除缓存（不是更新）
3. 防护缓存穿透、击穿、雪崩
4. 使用批量操作减少网络开销
5. 监控缓存命中率和内存使用

合理使用 Redis 缓存，可以显著提升系统性能！🚀
