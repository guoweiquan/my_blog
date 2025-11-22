# 接口文档规范

## 目录
- [Swagger/OpenAPI 配置](#swaggeropenapi-配置)
- [接口文档标准](#接口文档标准)
- [在线调试环境](#在线调试环境)
- [最佳实践](#最佳实践)

---

## Swagger/OpenAPI 配置

### 1. Maven 依赖

```xml
<dependencies>
    <!-- SpringDoc OpenAPI UI -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.2.0</version>
    </dependency>
    
    <dependency>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-annotations</artifactId>
        <version>2.2.16</version>
    </dependency>
</dependencies>
```

### 2. 配置类

```java
package com.blog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("博客系统 API")
                .version("1.0.0")
                .description("博客系统 RESTful API 文档")
                .contact(new Contact()
                    .name("开发团队")
                    .email("dev@blog.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .addServersItem(new Server().url("http://localhost:8080").description("本地环境"))
            .addServersItem(new Server().url("https://api.blog.com").description("生产环境"))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("01-公开接口")
            .pathsToMatch("/api/posts/**", "/api/categories/**", "/api/tags/**")
            .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("02-认证接口")
            .pathsToMatch("/api/auth/**")
            .build();
    }

    @Bean
    public GroupedOpenApi commentApi() {
        return GroupedOpenApi.builder()
            .group("03-评论接口")
            .pathsToMatch("/api/comments/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("04-用户接口")
            .pathsToMatch("/api/users/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("05-管理接口")
            .pathsToMatch("/api/admin/**")
            .build();
    }
}
```

### 3. application.yml 配置

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    persist-authorization: true
    display-request-duration: true
    tags-sorter: alpha
    operations-sorter: alpha
    
  api-docs:
    path: /v3/api-docs
    enabled: true
    
  default-produces-media-type: application/json
  default-consumes-media-type: application/json
```

---

## 接口文档标准

### 1. Controller 注解示例

```java
package com.blog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@Tag(name = "文章管理", description = "文章的增删改查接口")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Operation(
        summary = "获取文章详情",
        description = "根据文章 slug 获取文章详细信息"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @GetMapping("/{slug}")
    public ResponseEntity<PostDTO> getPost(
        @Parameter(description = "文章别名", example = "spring-boot-tutorial")
        @PathVariable String slug
    ) {
        // 实现代码
    }

    @Operation(
        summary = "创建文章",
        description = "需要管理员或作者权限"
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping
    public ResponseEntity<PostDTO> createPost(
        @RequestBody @Valid PostCreateRequest request
    ) {
        // 实现代码
    }

    @Operation(summary = "文章列表", description = "支持分页和筛选")
    @GetMapping
    public ResponseEntity<PageResult<PostDTO>> listPosts(
        @Parameter(description = "页码", example = "1")
        @RequestParam(defaultValue = "1") Integer page,
        
        @Parameter(description = "每页数量", example = "10")
        @RequestParam(defaultValue = "10") Integer size,
        
        @Parameter(description = "分类ID")
        @RequestParam(required = false) Long categoryId,
        
        @Parameter(description = "标签名称")
        @RequestParam(required = false) String tag,
        
        @Parameter(description = "搜索关键词")
        @RequestParam(required = false) String keyword
    ) {
        // 实现代码
    }
}
```

### 2. DTO 注解示例

```java
package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.*;

@Schema(description = "文章创建请求")
public class PostCreateRequest {

    @Schema(description = "文章标题", example = "Spring Boot 入门教程", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @Schema(description = "文章别名", example = "spring-boot-tutorial")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "别名只能包含小写字母、数字和连字符")
    private String slug;

    @Schema(description = "文章摘要", example = "本文介绍 Spring Boot 的基本使用")
    @Size(max = 500, message = "摘要长度不能超过500")
    private String summary;

    @Schema(description = "文章内容（Markdown）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "分类ID", example = "1")
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Schema(description = "标签ID列表", example = "[1, 2, 3]")
    private List<Long> tagIds;

    @Schema(description = "封面图URL", example = "https://example.com/cover.jpg")
    private String coverImage;

    @Schema(description = "发布状态", example = "PUBLISHED", allowableValues = {"DRAFT", "PUBLISHED"})
    private String status;

    // Getters and Setters
}
```

### 3. 统一响应格式

```java
package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一响应格式")
public class ApiResponse<T> {

    @Schema(description = "响应码", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "success")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳", example = "1700000000000")
    private Long timestamp;

    // 构造方法和静态工厂方法
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}
```

---

## 在线调试环境

### 1. 访问地址

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

### 2. 认证配置

1. 点击右上角 **Authorize** 按钮
2. 输入 JWT Token（格式：`Bearer eyJhbGciOiJIUzI1NiIs...`）
3. 点击 **Authorize** 确认
4. 所有需要认证的接口会自动携带 Token

### 3. 接口测试步骤

```
1. 选择接口分组（如：01-公开接口）
2. 展开具体接口（如：GET /api/posts/{slug}）
3. 点击 "Try it out" 按钮
4. 填写请求参数
5. 点击 "Execute" 执行请求
6. 查看响应结果
```

### 4. Mock 数据配置

```java
@Configuration
@Profile("dev")
public class SwaggerMockConfig {

    @Bean
    public ExampleProvider exampleProvider() {
        return new ExampleProvider() {
            @Override
            public Optional<Example> findExample(MediaType mediaType, Schema schema) {
                // 自定义 Mock 数据
                if (schema.getName() != null && schema.getName().equals("PostDTO")) {
                    Example example = new Example();
                    example.setValue(createMockPost());
                    return Optional.of(example);
                }
                return Optional.empty();
            }
        };
    }

    private PostDTO createMockPost() {
        PostDTO post = new PostDTO();
        post.setId(1L);
        post.setTitle("Spring Boot 入门教程");
        post.setSlug("spring-boot-tutorial");
        post.setSummary("本文介绍 Spring Boot 的基本使用");
        post.setCreatedAt(LocalDateTime.now());
        return post;
    }
}
```

---

## 最佳实践

### 1. 接口命名规范

| 操作 | HTTP 方法 | 路径示例 | 说明 |
|------|----------|---------|------|
| 列表 | GET | /api/posts | 获取文章列表 |
| 详情 | GET | /api/posts/{id} | 获取单篇文章 |
| 创建 | POST | /api/posts | 创建文章 |
| 更新 | PUT | /api/posts/{id} | 完整更新 |
| 部分更新 | PATCH | /api/posts/{id} | 部分更新 |
| 删除 | DELETE | /api/posts/{id} | 删除文章 |
| 批量操作 | POST | /api/posts/batch | 批量操作 |

### 2. 状态码使用

| 状态码 | 说明 | 使用场景 |
|--------|------|---------|
| 200 | OK | 成功（GET/PUT/PATCH） |
| 201 | Created | 创建成功（POST） |
| 204 | No Content | 删除成功（DELETE） |
| 400 | Bad Request | 参数错误 |
| 401 | Unauthorized | 未认证 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突 |
| 429 | Too Many Requests | 请求过于频繁 |
| 500 | Internal Server Error | 服务器错误 |

### 3. 错误响应格式

```java
@Schema(description = "错误响应")
public class ErrorResponse {

    @Schema(description = "错误码", example = "VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "错误消息", example = "参数校验失败")
    private String message;

    @Schema(description = "字段错误详情")
    private Map<String, String> fieldErrors;

    @Schema(description = "时间戳")
    private Long timestamp;

    @Schema(description = "请求路径")
    private String path;
}
```

### 4. 分页参数标准

```java
@Schema(description = "分页请求")
public class PageRequest {

    @Schema(description = "页码，从1开始", example = "1", minimum = "1")
    @Min(1)
    private Integer page = 1;

    @Schema(description = "每页数量", example = "10", minimum = "1", maximum = "100")
    @Min(1)
    @Max(100)
    private Integer size = 10;

    @Schema(description = "排序字段", example = "createdAt")
    private String sortBy = "createdAt";

    @Schema(description = "排序方向", example = "DESC", allowableValues = {"ASC", "DESC"})
    private String sortDir = "DESC";
}

@Schema(description = "分页响应")
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> items;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Integer page;

    @Schema(description = "每页数量", example = "10")
    private Integer size;

    @Schema(description = "总页数", example = "10")
    private Integer totalPages;

    @Schema(description = "是否有下一页", example = "true")
    private Boolean hasNext;

    @Schema(description = "是否有上一页", example = "false")
    private Boolean hasPrev;
}
```

### 5. 导出 API 文档

#### 生成 HTML 文档

```xml
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>1.4</version>
    <executions>
        <execution>
            <id>generate-openapi</id>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <apiDocsUrl>http://localhost:8080/v3/api-docs</apiDocsUrl>
        <outputFileName>openapi.json</outputFileName>
        <outputDir>${project.build.directory}</outputDir>
    </configuration>
</plugin>
```

#### 生成 Postman Collection

```bash
# 下载 OpenAPI JSON
curl http://localhost:8080/v3/api-docs > openapi.json

# 使用 openapi-to-postmanv2 转换
npx openapi-to-postmanv2 -s openapi.json -o postman-collection.json
```

### 6. 环境隔离

```yaml
# application-dev.yml
springdoc:
  swagger-ui:
    enabled: true
  api-docs:
    enabled: true

# application-prod.yml  
springdoc:
  swagger-ui:
    enabled: false  # 生产环境关闭 UI
  api-docs:
    enabled: true   # 但保留 API 文档（供内部使用）
```

---

## 常见问题

### Q1: 如何隐藏某些接口？

```java
@Hidden  // 隐藏整个 Controller
@RestController
public class InternalController {
    // ...
}

@Operation(hidden = true)  // 隐藏单个接口
@GetMapping("/internal")
public String internal() {
    // ...
}
```

### Q2: 如何自定义示例值？

```java
@Schema(description = "用户ID", example = "123")
private Long id;

// 或使用 @ExampleObject
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    content = @Content(
        examples = @ExampleObject(
            name = "创建文章示例",
            value = "{\"title\": \"测试文章\", \"content\": \"内容\"}"
        )
    )
)
```

### Q3: 如何处理文件上传？

```java
@Operation(summary = "上传图片")
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<String> uploadImage(
    @Parameter(description = "图片文件", required = true,
        content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    @RequestPart("file") MultipartFile file
) {
    // 实现代码
}
```

---

## 总结

通过 SpringDoc OpenAPI 3.0，我们可以：

1. ✅ 自动生成 API 文档
2. ✅ 提供在线调试界面
3. ✅ 支持多种导出格式
4. ✅ 集成 JWT 认证
5. ✅ 按模块分组管理
6. ✅ 生产环境可控制开关

**访问地址**: http://localhost:8080/swagger-ui.html
