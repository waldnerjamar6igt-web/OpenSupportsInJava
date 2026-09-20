# OpenSupports Backend Implementation Plan

## Goal

完成 openSupports 全部后端代码实现：数据库 Schema、实体类、MyBatis Mapper、Domain 层业务逻辑、异常体系、鉴权拦截器、Controller 路由与全局异常处理，使项目从空壳变成可编译运行的完整 REST API。

---

## Current Context / Assumptions

### 技术栈（已确认）
- **框架**: Spring Boot 4.0.8 + Java 21
- **持久化**: MyBatis (mybatis-spring-boot-starter 4.0.1)，用户要求使用 **SQLite**
- **依赖**: lombok, spring-boot-starter-web, spring-boot-starter-validation, spring-session-jdbc, mysql-connector-j
- **包路径**: `com.openSupports.demo`
- **API 前缀**: `/api/v1`

### 项目现状（逐文件审查结果）

| 层级 | 状态 | 详情 |
|------|------|------|
| pom.xml | ⚠️ 需修改 | 有 `spring-boot-starter-validation`；MySQL connector 需要替换为 SQLite JDBC |
| Domain/Model/*.java | ❌ 空壳 | Account/Ticket/Department/Article/Tag 只有方法签名，无字段无业务逻辑 |
| Domain/event/*.java | ⚠️ 半成品 | Event 基类存在但无 payload；StaffDeleted/TagDeleted 继承 Event；StaffDisabled 未继承 Event |
| Domain/repo/*.java | ❌ 空壳 | AccountRepo/TicketRepo/KnowledgeBaseRepo/Repo 只有空方法 |
| Domain/service/*.java | ❌ 空壳 | TicketDomainService 只有签名，无校验逻辑 |
| api/dto/*/*.java | ✅ 已完成 | 所有 DTO 定义完整，含 jakarta.validation 注解 |
| application/service/*.java | ⚠️ 骨架但有 bug | 方法调用链不完整，有未定义类型（TicketBrief/TicketBrif），缺少 @Component/@Autowired |
| application/controller/*.java | ❌ 骨架但有 bug | 无 @RestController/@RequestMapping/@PostMapping 等注解；构造函数语法错误 `AccountController()` |
| application/handler/*.java | ⚠️ 部分 | AccountHandler 为空；TicketHandler 有 @EventListener 但逻辑粗糙（highLvSrch 取全表） |
| infra/mapper/ | ❌ 不存在 | MyBatis Mapper 目录为空 |
| infra/exception/ | ❌ 不存在 | 异常类目录不存在 |
| application/interceptor/ | ❌ 不存在 | 鉴权拦截器不存在 |
| resources/application.yaml | ❌ 需大量修改 | 仅 3 行基础配置 |

### BA 覆盖范围（UC-01 ~ UC-18 全部实现）

```
UC-01: 用户注册        → POST /api/v1/auth/signup
UC-02: 账户登录        → POST /api/v1/auth/signin
UC-03: 个人资料管理     → PUT /api/v1/account/email, password
UC-04: 创建工单        → POST /api/v1/tickets
UC-05: 工单粗略视图     → GET /api/v1/tickets/me-assigned/sent/new/all/search
UC-06: 工单具体视图     → GET /api/v1/tickets/{id}
UC-07: 回复工单        → POST /api/v1/tickets/{id}/comments
UC-08: 编辑工单        → PUT /api/v1/tickets/{id}/title, PUT /api/v1/tickets/comments/{commentId}
UC-09: 工单标签        → POST/DELETE /api/v1/tickets/{id}/tags/{tagId}
UC-10: 工单管理        → PUT ticket/{id}/assign/unassign/department/close/reopen, DELETE ticket/{id}
UC-11: 高级搜索        → GET /api/v1/tickets/advanced-search
UC-12: 用户搜索治理     → GET /api/v1/admin/users, /api/v1/admin/users/{id}
UC-13: Staff 搜索      → GET /api/v1/admin/staff
UC-14: 员工管理        → POST/PUT/DELETE /api/v1/admin/staff
UC-15: 用户管理        → POST/PUT/DELETE /api/v1/admin/users
UC-16: 知识库概览/主题  → GET /api/v1/kb/folders, CRUD /api/v1/kb/folders/{id}
UC-17: 文章管理        → CRUD /api/v1/kb/articles
UC-18: 部门管理        → CRUD /api/v1/departments, migration
```

---

## Architecture / Proposed Approach

采用 **"底层向上构建"** 策略：Schema → Entity → Mapper → Repo Interface → Impl → Domain Model Logic → Service Layer → Interceptor → Controller → Global Exception Handler。每个模块完成后立即编译验证，避免最后出现连锁编译错误。SQLite 提供嵌入式零配置数据库能力，通过 MyBatis Plus 的通用 Mapper 减少样板代码但本项目不使用 MP，纯手写 MyBatis XML Mapper 以保持对 DDD 仓库模式的清晰分离。

---

## Step-by-Step Tasks

### Phase 0: 基础设施准备

#### Task 0.1: 修改 pom.xml — 添加 SQLite 和 MyBatis 必要依赖

**文件**: `demo/pom.xml`

将 `mysql-connector-j` 替换为 `sqlite-jdbc` 和 `log4j`（SQLite 日志需求），并添加必要的额外依赖：

```xml
<!-- REPLACE mysql-connector-j with SQLite dependencies -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter-test</artifactId>
    <version>3.0.3</version>  <!-- Downgrade for Spring Boot 3.x compat; check Boot 4.0 compatibility -->
    <scope>test</scope>
</dependency>
```

> **注意**: Spring Boot 4.0.8 是较新版本，需要确认 mybatis-spring-boot-starter 版本兼容。如果 4.0.1 不匹配测试场景，降级到 3.0.3。如果 Spring Boot 4.0 尚未发布实际 artifact，可能需要先用 3.x 系列过渡。

**验证**: `cd demo && mvn dependency:resolve -q && echo "RESOLVED OK"`

#### Task 0.2: 创建目录结构

```bash
cd demo/src/main/java/com/openSupports/demo
mkdir -p Domain/event
mkdir -p infra/mapper
mkdir -p infra/exception
mkdir -p application/interceptor
```

#### Task 0.3: 创建 resources/db/schema.sql

**文件**: `demo/src/main/resources/db/schema.sql`

完整的 SQLite DDL，包含所有表的 CREATE TABLE、索引和外键约束：

```sql
-- users table (customer accounts)
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    state TEXT DEFAULT 'ENABLED' CHECK(state IN ('ENABLED', 'DISABLED')),
    sign_up_time TEXT NOT NULL DEFAULT (datetime('now')),
    last_login_time TEXT,
    remember_token TEXT,
    remember_token_expires TEXT,
    csrf_userid TEXT NOT NULL DEFAULT '',
    csrf_token TEXT NOT NULL DEFAULT ''
);

-- staff table (employee accounts, inherits from users conceptually but separate for level/department)
CREATE TABLE IF NOT EXISTS staff (
    id INTEGER PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    level INTEGER NOT NULL CHECK(level BETWEEN 1 AND 3),
    department_id INTEGER NOT NULL,
    state TEXT DEFAULT 'ENABLED' CHECK(state IN ('ENABLED', 'DISABLED')),
    sign_up_time TEXT NOT NULL DEFAULT (datetime('now')),
    last_login_time TEXT,
    remember_token TEXT,
    remember_token_expires TEXT,
    csrf_userid TEXT NOT NULL DEFAULT '',
    csrf_token TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- departments
CREATE TABLE IF NOT EXISTS departments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    is_default INTEGER NOT NULL DEFAULT 0,
    is_private INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- tickets
CREATE TABLE IF NOT EXISTS tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_code TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN', 'CLOSED', 'REOPENED')),
    priority TEXT NOT NULL DEFAULT 'NORMAL',  -- LOW/NORMAL/HIGH/URGENT
    department_id INTEGER NOT NULL,
    author_id INTEGER NOT NULL,
    assignee_id INTEGER,
    unread_staff INTEGER NOT NULL DEFAULT 1,
    closed INTEGER NOT NULL DEFAULT 0,
    edited_title INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    last_activity_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (department_id) REFERENCES departments(id),
    FOREIGN KEY (author_id) REFERENCES users(id),
    FOREIGN KEY (assignee_id) REFERENCES staff(id)
);

-- ticket_comments
CREATE TABLE IF NOT EXISTS ticket_comments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id INTEGER NOT NULL,
    author_id INTEGER NOT NULL,
    author_type TEXT NOT NULL CHECK(author_type IN ('USER', 'STAFF')),
    content TEXT NOT NULL,
    is_private INTEGER NOT NULL DEFAULT 0,
    is_edited INTEGER NOT NULL DEFAULT 0,
    edited_by INTEGER,
    edited_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- tags (global tags, attached to tickets)
CREATE TABLE IF NOT EXISTS tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ticket_tags (many-to-many)
CREATE TABLE IF NOT EXISTS ticket_tags (
    ticket_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (ticket_id, tag_id),
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- knowledge_folders (topics/categories)
CREATE TABLE IF NOT EXISTS knowledge_folders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    is_private INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- knowledge_articles
CREATE TABLE IF NOT EXISTS knowledge_articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    folder_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (folder_id) REFERENCES knowledge_folders(id) ON DELETE SET NULL
);

-- attachments (file metadata for tickets/comments)
CREATE TABLE IF NOT EXISTS attachments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_type TEXT NOT NULL CHECK(owner_type IN ('TICKET', 'COMMENT')),
    owner_id INTEGER NOT NULL,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_size INTEGER NOT NULL DEFAULT 0,
    content_type TEXT,
    uploaded_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- session table (for spring-session-jdbc)
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);
CREATE INDEX SPRING_SESSION_IX4 ON SPRING_SESSION (LAST_ACCESS_TIME);
```

**验证**: `sqlite3 /tmp/test_opensupports.db < src/main/resources/db/schema.sql && sqlite3 /tmp/test_opensupports.db ".tables"` 应输出所有表名。

#### Task 0.4: 修改 application.yaml

**文件**: `demo/src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: opensupports
  datasource:
    url: jdbc:sqlite:${APP_DATA_DIR:~/opensupports}/db.sqlite
    driver-class-name: org.sqlite.JDBC
    hikari:
      connection-timeout: 30000
      maximum-pool-size: 5
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
  session:
    store-type: jdbc
    timeout: 30m

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.openSupports.demo.Domain.Model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

server:
  port: 8080

app:
  jwt:
    enabled: false  # Using session-based auth for now
  upload-dir: ${UPLOAD_DIR:~/opensupports/uploads}
```

---

### Phase 1: 异常体系 & 常量

#### Task 1.1: 创建异常类

**文件**: `demo/src/main/java/com/openSupports/demo/infra/exception/AppException.java`

```java
package com.openSupports.demo.infra.exception;

public class AppException extends RuntimeException {
    private final String errorCode;

    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/infra/exception/BusinessRuleViolationException.java`

```java
package com.openSupports.demo.infra.exception;

public class BusinessRuleViolationException extends AppException {
    public BusinessRuleViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/infra/exception/ResourceNotFoundException.java`

```java
package com.openSupports.demo.infra.exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, Long id) {
        super("RESOURCE_NOT_FOUND", resource + " not found with id: " + id);
    }
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/infra/exception/PermissionDeniedException.java`

```java
package com.openSupports.demo.infra.exception;

public class PermissionDeniedException extends AppException {
    public PermissionDeniedException(String message) {
        super("PERMISSION_DENIED", message);
    }
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/infra/exception/GlobalExceptionHandler.java`

```java
package com.openSupports.demo.infra.exception;

import com.openSupports.demo.api.dto.common.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(ResourceNotFoundException e) {
        return new ApiError(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBusinessRule(BusinessRuleViolationException e) {
        return new ApiError(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handlePermission(PermissionDeniedException e) {
        return new ApiError(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return new ApiError("VALIDATION_ERROR", details);
    }

    @ExceptionHandler(AppException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleAppException(AppException e) {
        return new ApiError(e.getErrorCode(), e.getMessage());
    }
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/infra/util/Constants.java`

```java
package com.openSupports.demo.infra.util;

public final class Constants {
    private Constants() {}

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 200;
    public static final long REMEMBER_TOKEN_EXPIRY_DAYS = 30;

    // Department
    public static final String DEPT_DEFAULT_NAME = "Software Support";

    // Ticket status constants
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REOPENED = "REOPENED";

    // Auth
    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
}
```

**验证**: 编译通过 `mvn compile -q`

---

### Phase 2: MyBatis Entity 类 & Mapper XML

#### Task 2.1: 重写 Entity 类 — 带完整字段

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/User.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private String salt;
    private String state = "ENABLED";
    private LocalDateTime signUpTime;
    private LocalDateTime lastLoginTime;
    private String rememberToken;
    private LocalDateTime rememberTokenExpires;
    private String csrfUserid;
    private String csrfToken;

    // Transient / computed fields
    private Integer ticketCount;
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Staff.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Staff extends User {
    private Integer level;           // 1=L1, 2=L2, 3=L3/Admin
    private Long departmentId;
    private String departmentName;   // denormalized for queries
    private Integer ticketAssignedCount;
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Department.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Department {
    private Long id;
    private String name;
    private Boolean isDefault = false;
    private Boolean isPrivate = false;
    private LocalDateTime createdAt;
    private Integer ticketCount;
    private Integer staffCount;
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Ticket.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Ticket {
    private Long id;
    private String ticketCode;
    private String title;
    private String content;          // initial description
    private String status = "OPEN";
    private String priority = "NORMAL";
    private Long departmentId;
    private String departmentName;   // denormalized
    private Long authorId;
    private String authorEmail;      // denormalized
    private Long assigneeId;
    private String assigneeName;     // denormalized
    private Boolean unreadStaff = true;
    private Boolean closed = false;
    private Boolean editedTitle = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActivityAt;

    // Aggregated children
    private List<Comment> comments;
    private List<Tag> tags;

    // Transient
    private Integer unreadCount;  // for current viewer
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Comment.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorType;     // "USER" or "STAFF"
    private String content;
    private Boolean isPrivate = false;
    private Boolean isEdited = false;
    private Long editedBy;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Denormalized for views
    private String authorName;
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Tag.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tag {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Folder.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Folder {
    private Long id;
    private String name;
    private Boolean isPrivate = false;
    private Integer sortOrder = 0;
    private LocalDateTime createdAt;
    private List<Article> articles;
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Article.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Article {
    private Long id;
    private String title;
    private String content;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Attachment.java`

```java
package com.openSupports.demo.Domain.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Attachment {
    private Long id;
    private String ownerType;  // "TICKET" or "COMMENT"
    private Long ownerId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}
```

#### Task 2.2: 创建 MyBatis Mapper XML 文件

**文件**: `demo/src/main/resources/mapper/UserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.openSupports.demo.infra.mapper.UserMapper">

    <resultMap id="userResultMap" type="com.openSupports.demo.Domain.Model.User">
        <id property="id" column="id"/>
        <result property="email" column="email"/>
        <result property="passwordHash" column="password_hash"/>
        <result property="salt" column="salt"/>
        <result property="state" column="state"/>
        <result property="signUpTime" column="sign_up_time"/>
        <result property="lastLoginTime" column="last_login_time"/>
        <result property="rememberToken" column="remember_token"/>
        <result property="rememberTokenExpires" column="remember_token_expires"/>
        <result property="csrfUserid" column="csrf_userid"/>
        <result property="csrfToken" column="csrf_token"/>
        <result property="ticketCount" column="ticket_count"/>
    </resultMap>

    <select id="findByEmail" resultMap="userResultMap">
        SELECT u.*, COUNT(t.id) as ticket_count
        FROM users u
        LEFT JOIN tickets t ON t.author_id = u.id
        WHERE u.email = #{email}
        GROUP BY u.id
    </select>

    <select id="findById" resultMap="userResultMap">
        SELECT * FROM users WHERE id = #{id}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (email, password_hash, salt, state, csrf_userid, csrf_token)
        VALUES (#{email}, #{passwordHash}, #{salt}, #{state}, #{csrfUserid}, #{csrfToken})
    </insert>

    <update id="updatePassword">
        UPDATE users SET password_hash = #{passwordHash}, salt = #{salt}, last_login_time = datetime('now')
        WHERE id = #{id}
    </update>

    <update id="changeEmail">
        UPDATE users SET email = #{email} WHERE id = #{id} AND email != #{excludeEmail}
    </update>

    <update id="setState">
        UPDATE users SET state = #{state} WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM users WHERE id = #{id}
    </delete>

    <select id="searchByKeyword" resultMap="userResultMap">
        SELECT u.*, COUNT(t.id) as ticket_count
        FROM users u
        LEFT JOIN tickets t ON t.author_id = u.id
        <where>
            <if test="emailKeyword != null and emailKeyword != ''">
                AND u.email LIKE '%' || #{emailKeyword} || '%'
            </if>
            <if test="registerDateFrom != null">
                AND u.sign_up_time >= #{registerDateFrom}
            </if>
            <if test="registerDateTo != null">
                AND u.sign_up_time &lt;= #{registerDateTo}
            </if>
        </where>
        GROUP BY u.id
        ORDER BY
            <choose>
                <when test="sortBy == 'ticketCount'">ticket_count DESC</when>
                <otherwise>u.sign_up_time DESC</otherwise>
            </choose>
        LIMIT #{pageSize} OFFSET #{offset}
    </select>

    <select id="countSearchResults" resultType="int">
        SELECT COUNT(*) FROM users
        <where>
            <if test="emailKeyword != null and emailKeyword != ''">
                AND email LIKE '%' || #{emailKeyword} || '%'
            </if>
            <if test="registerDateFrom != null">
                AND sign_up_time >= #{registerDateFrom}
            </if>
            <if test="registerDateTo != null">
                AND sign_up_time &lt;= #{registerDateTo}
            </if>
        </where>
    </select>

    <select id="findAll" resultMap="userResultMap">
        SELECT u.*, COUNT(t.id) as ticket_count FROM users u
        LEFT JOIN tickets t ON t.author_id = u.id
        GROUP BY u.id ORDER BY u.sign_up_time DESC
        LIMIT #{pageSize} OFFSET #{offset}
    </select>

    <select id="countAll" resultType="int">
        SELECT COUNT(*) FROM users
    </select>

    <select id="findEnabledUsers" resultMap="userResultMap">
        SELECT * FROM users WHERE state = 'ENABLED' ORDER BY sign_up_time DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>
</mapper>
```

类似的 Mapper XML 需要为每个 Entity 创建：

| 文件 | 核心查询 |
|------|---------|
| `resources/mapper/StafaMapper.xml` | findByEmail, findById, insert, updatePassword, changeDepartment, setState(ENABLED/DISABLED), deleteById, searchWithFilters(emailKeyword/departmentId/level/dateRange), findAllStaff, countSearchResults |
| `resources/mapper/DepartmentMapper.xml` | findAll, findById, findByName, insert (name unique check), updateName, delete (with existence checks), findDefaultDept, migrateStaff/update department_id for users in dept, migrateTickets/update department_id |
| `resources/mapper/TicketMapper.xml` | findById with join to author/dept, createTicket(generate ticketCode), save, highLvSrch with filters, searchMeSent, searchMyAssigned, searchNewForDept, searchAllForDept, searchByTitle, advancedSearch(complex multi-condition), closeTicket, reopenTicket, deleteTicket, changeDepartment, unAssign, assignTo, updateUnreadStaff, getLastCommentForTicket, countByAuthor, countByAssignee, searchAdvanced(title/tags/ticketCode/closed/dateRange/departmentId/authorId/ownerId/assigned/query) |
| `resources/mapper/CommentMapper.xml` | insertComment, findById, updateContent, isLatestComment(ticketId), findByTicketId, deleteComment |
| `resources/mapper/TagMapper.xml` | findByTicketId, insert, insertIfNotExists + selectId, deleteById, deleteAllForTicket, findByNameUnique, countDistinctTags |
| `resources/mapper/FolderMapper.xml` | findAllPublic, findAllIncludingPrivate, findById, insert, updateName, updateState(isPrivate), updateOrder, deleteFolder (move articles to NULL folder_id) |
| `resources/mapper/ArticleMapper.xml` | findByFolderId(public only filter), findById, insert, update, delete |

**关键设计决策**:
- 使用 `<if>` 动态 SQL 构建复杂搜索条件
- 所有分页查询都接受 `pageSize` + `offset` 参数（修复 BA 中 OFFSET 硬编码问题）
- 外键关系通过显式 JOIN 或二级查询实现，保持 SQLite 兼容性
- `useGeneratedKeys="true"` 用于自增 ID

#### Task 2.3: 创建 Mapper Java 接口

```java
// UserMapper.java
package com.openSupports.demo.infra.mapper;
import com.openSupports.demo.Domain.Model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.time.LocalDate;

@Mapper
public interface UserMapper {
    User findByEmail(@Param("email") String email);
    User findById(@Param("id") Long id);
    int insert(User user);
    int updatePassword(User user);
    int changeEmail(@Param("email") String email, @Param("id") Long id, @Param("excludeEmail") String excludeEmail);
    int setState(@Param("id") Long id, @Param("state") String state);
    int deleteById(@Param("id") Long id);

    // Search
    List<User> searchByKeyword(@Param("emailKeyword") String keyword, @Param("registerDateFrom") LocalDate from,
                               @Param("registerDateTo") LocalDate to, @Param("sortBy") String sortBy,
                               @Param("offset") int offset, @Param("pageSize") int pageSize);
    int countSearchResults(@Param("emailKeyword") String keyword, @Param("registerDateFrom") LocalDate from,
                           @Param("registerDateTo") LocalDate to);
    List<User> findAll(@Param("offset") int offset, @Param("pageSize") int pageSize);
    int countAll();
}
```

类似接口为 StaffMapper, DepartmentMapper, TicketMapper, CommentMapper, TagMapper, FolderMapper, ArticleMapper 各创建一个。StaffMapper extends UserMapper 或在同一个 XML namespace 下共享。

**验证**: `mvn compile -q` 确保所有 Mapper 接口和 XML 解析正常。

---

### Phase 3: Domain 层完善

#### Task 3.1: 填充 Account 聚合 — 业务行为

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Account.java`

根据 BA 建模文档重构 Account 为一个真正的聚合根，包含领域事件收集：

```java
package com.openSupports.demo.Domain.Model;

import com.openSupports.demo.Domain.event.*;
import com.openSupports.demo.infra.exception.BusinessRuleViolationException;
import lombok.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class Account {
    private Set<Event> events = new HashSet<>();

    // === Command Methods (State Transitions) ===

    public void createUser(String email, String passwordHash, String salt) {
        if (this.email != null && !this.email.isEmpty()) {
            throw new BusinessRuleViolationException("ACCOUNT_ALREADY_EXISTS", "邮箱已被注册");
        }
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.state = "ENABLED";
        this.csrfUserid = UUID.randomUUID().toString().substring(0, 8);
        this.csrfToken = UUID.randomUUID().toString();
        publishEvent(new UserRegistered());
    }

    public void initAsStaff(Integer level, Long departmentId) {
        if (this.level != null) {
            throw new BusinessRuleViolationException("NOT_A_USER_ACCOUNT", "该账户已是员工账号");
        }
        this.level = level;
        this.departmentId = departmentId;
        publishEvent(new StaffCreated());
    }

    public void changePassword(String newPasswordHash, String newSalt) {
        validateActive();
        this.passwordHash = newPasswordHash;
        this.salt = newSalt;
        publishEvent(new PasswordChanged());
    }

    public void editEmailAddress(String newEmail, String currentEmail) {
        validateActive();
        if (newEmail.equals(currentEmail)) {
            throw new BusinessRuleViolationException("EMAIL_SAME_AS_CURRENT", "新邮箱与当前邮箱相同");
        }
        this.email = newEmail;
        publishEvent(new EmailAddressChanged());
    }

    public void disable() {
        if ("DISABLED".equals(this.state)) {
            throw new BusinessRuleViolationException("ACCOUNT_ALREADY_DISABLED", "账户已被停用");
        }
        this.state = "DISABLED";
        publishEvent(new StaffDisabledEvent());
    }

    public void enable() {
        if ("ENABLED".equals(this.state)) {
            throw new BusinessRuleViolationException("ACCOUNT_ALREADY_ENABLED", "账户已启用");
        }
        this.state = "ENABLED";
        publishEvent(new StaffEnabled());
    }

    public void deleteSelf() {
        publishEvent(new StaffDeleted());
    }

    public void updateRememberToken(String token, java.time.LocalDateTime expires) {
        this.rememberToken = token;
        this.rememberTokenExpires = expires;
    }

    public void recordLogin() {
        this.lastLoginTime = java.time.LocalDateTime.now();
    }

    // === Utility Methods ===

    public boolean isEnabled() {
        return "ENABLED".equals(this.state);
    }

    public boolean isStaff() {
        return this.level != null;
    }

    public Integer getLevel() {
        return isStaff() ? this.level : 0;  // 0 = non-staff
    }

    public boolean isAdmin() {
        return Integer.valueOf(3).equals(this.level);
    }

    public boolean isSupervisor() {
        return Integer.valueOf(2).equals(this.level);
    }

    // === Domain Events ===

    private void publishEvent(Event evt) {
        this.events.add(evt);
    }

    // === Validation ===

    private void validateActive() {
        if (!isEnabled()) {
            throw new BusinessRuleViolationException("ACCOUNT_DISABLED", "账户已被停用，无法执行此操作");
        }
    }

    // Clear events after publishing
    public void clearEvents() {
        this.events.clear();
    }
}
```

#### Task 3.2: 创建领域事件类

需要补充的事件类（之前缺失的）：

```java
// Domain/event/UserRegistered.java
package com.openSupports.demo.Domain.event;
public class UserRegistered extends Event {
    public final String email;
    public UserRegistered(String email) { this.email = email; }
}

// Domain/event/StaffCreated.java
package com.openSupports.demo.Domain.event;
public class StaffCreated extends Event {
    public final Integer level;
    public final Long departmentId;
    public StaffCreated(Integer level, Long departmentId) {
        this.level = level;
        this.departmentId = departmentId;
    }
}

// Domain/event/PasswordChanged.java
package com.openSupports.demo.Domain.event;
public class PasswordChanged extends Event {}

// Domain/event/EmailAddressChanged.java
package com.openSupports.demo.Domain.event;
public class EmailAddressChanged extends Event {}

// Domain/event/StaffEnabled.java
package com.openSupports.demo.Domain.event;
public class StaffEnabled extends Event {}

// Existing: StaffDeleted, StaffDisabled -> fix StaffDisabled to extend Event, TagDeleted already extends Event
```

#### Task 3.3: 填充 Ticket 聚合 — 完整业务行为

**文件**: `demo/src/main/java/com/openSupports/demo/Domain/Model/Ticket.java`

```java
package com.openSupports.demo.Domain.Model;

import com.openSupports.demo.Domain.event.TagDeleted;
import com.openSupports.demo.infra.exception.BusinessRuleViolationException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class Ticket extends Account {  // Reuses some base fields

    private String ticketCode;
    private String title;
    private String content;
    private String status = "OPEN";
    private String priority = "NORMAL";
    private Long departmentId;
    private Long authorId;
    private String authorEmail;
    private Long assigneeId;
    private String assigneeName;
    private Boolean unreadStaff = true;
    private Boolean closed = false;
    private Boolean editedTitle = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActivityAt;

    private List<Comment> comments = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    // === Commands ===

    public void createTicket(Long deptId, Long authorId, String authorEmail) {
        this.departmentId = deptId;
        this.authorId = authorId;
        this.authorEmail = authorEmail;
        this.status = "OPEN";
        this.closed = false;
        this.unreadStaff = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        // ticketCode will be set by repo
    }

    public void addComment(Comment comment, boolean isStaff) {
        comment.setTicketId(this.id);
        comment.setAuthorType(isStaff ? "STAFF" : "USER");
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        this.comments.add(comment);
        this.lastActivityAt = LocalDateTime.now();
        this.unreadStaff = true;
    }

    public void editTitle(String newTitle) {
        validateCanEdit();
        this.title = newTitle;
        this.editedTitle = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void editLatestComment(Long commentId, String newContent) {
        validateCanEdit();
        Comment latest = getLatestCommentForAuthor();
        if (latest == null || !latest.getId().equals(commentId)) {
            throw new BusinessRuleViolationException("TICKET_CONTENT_CANNOT_BE_EDITED", "只能编辑自己最新的一条评论");
        }
        latest.setContent(newContent);
        latest.setIsEdited(true);
        latest.setEditedAt(LocalDateTime.now());
        latest.setUpdatedAt(LocalDateTime.now());
    }

    public void addTag(Tag tag) {
        if (tags.stream().anyMatch(t -> t.getName().equalsIgnoreCase(tag.getName()))) {
            throw new BusinessRuleViolationException("TAG_EXISTS", "标签已存在于该工单");
        }
        tag.setId(null); // Will be generated
        tags.add(tag);
    }

    public void removeTag(Tag tag) {
        if (tags.removeIf(t -> t.getId().equals(tag.getId()))) {
            this.publishEvent(new TagDeleted());
        }
    }

    public void assignTo(Long staffId, String staffName) {
        validateOpen();
        this.assigneeId = staffId;
        this.assigneeName = staffName;
    }

    public void unAssign() {
        validateOpen();
        this.assigneeId = null;
        this.assigneeName = null;
    }

    public void changeDepartment(Long newDeptId) {
        unAssign(); // Unassign before department change per BA
        this.departmentId = newDeptId;
        this.updatedAt = LocalDateTime.now();
    }

    public void closeTicket() {
        validateOpen();
        this.status = "CLOSED";
        this.closed = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void reopenTicket() {
        if (!closed) {
            throw new BusinessRuleViolationException("TICKET_NOT_CLOSED", "工单未关闭，不能重新打开");
        }
        this.status = "REOPENED";
        this.closed = false;
        this.assigneeId = null;  // Auto-unassign on reopen
        this.assigneeName = null;
        this.unreadStaff = true;
        this.updatedAt = LocalDateTime.now();
    }

    // === Query Helpers ===

    public Comment getLatestCommentForAuthor() {
        return comments.stream()
                .filter(c -> !c.getIsEdited())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst()
                .orElse(null);
    }

    public Comment getLatestComment() {
        return comments.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst()
                .orElse(null);
    }

    // === Validation ===

    private void validateOpen() {
        if (closed) {
            throw new BusinessRuleViolationException("TICKET_CLOSED", "工单已关闭，无法执行此操作");
        }
    }

    private void validateCanEdit() {
        validateOpen();
    }
}
```

#### Task 3.4: 实现 Comment 模型

```java
// Already above in Task 3.3 — just make sure it's in its own file
// File: Domain/Model/Comment.java
package com.openSupports.demo.Domain.Model;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorType;     // "USER" or "STAFF"
    private String content;
    private Boolean isPrivate = false;
    private Boolean isEdited = false;
    private Long editedBy;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorName;       // denormalized from query
}
```

#### Task 3.5: 实现 Department 聚合

```java
// File: Domain/Model/Department.java
package com.openSupports.demo.Domain.Model;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Department {
    private Long id;
    private String name;
    private Boolean isDefault = false;
    private Boolean isPrivate = false;
    private LocalDateTime createdAt;
    private Integer ticketCount;
    private Integer staffCount;

    public void rename(String newName) {
        if (this.isDefault) {
            throw new com.openSupports.demo.infra.exception.BusinessRuleViolationException("CANNOT_RENAME_DEFAULT_DEPT", "不能重命名默认部门");
        }
        this.name = newName;
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void togglePrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
```

#### Task 3.6: 实现 Folder & Article 聚合

```java
// File: Domain/Model/Folder.java
package com.openSupports.demo.Domain.Model;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Folder {
    private Long id;
    private String name;
    private Boolean isPrivate = false;
    private Integer sortOrder = 0;
    private LocalDateTime createdAt;
    private List<Article> articles;

    public void rename(String newName) {
        this.name = newName;
    }

    public void togglePrivate() {
        this.isPrivate = !this.isPrivate;
    }
}

// File: Domain/Model/Article.java
package com.openSupports.demo.Domain.Model;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Article {
    private Long id;
    private String title;
    private String content;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateContent(String title, String content, Long folderId) {
        this.title = title;
        this.content = content;
        this.folderId = folderId;
        this.updatedAt = LocalDateTime.now();
    }
}
```

#### Task 3.7: 填充 Repo 接口 — 用 MyBatis 实现

每个原有的空 Repo 类改造成接口并用 MyBatis 注解方式或 XML 映射。推荐接口+XML 模式：

```java
// Domain/repo/AccountRepo.java → becomes an interface
package com.openSupports.demo.Domain.repo;
import com.openSupports.demo.Domain.Model.Account;
import java.util.List;
import java.time.LocalDate;

public interface AccountRepo {
    Account findByEmail(String email);
    Account findById(Long id);
    Account createAndSave(String email, String passwordHash, String salt);
    void updateAccount(Account account);
    void save(Account account);
    void changePassword(Long accountId, String hash, String salt);
    void changeEmail(String email, Long id, String excludeEmail);
    void setState(Long id, String state);
    void delete(Long id);
    List<Account> searchByEmailKeyword(String keyword, LocalDate from, LocalDate to, int offset, int size);
    int countSearchResults(String keyword, LocalDate from, LocalDate to);
    List<Account> findAll(int offset, int size);
    int countAll();
    Account createStaff(String email, String passwordHash, String salt, Integer level, Long deptId);
}
```

TicketRepo, DepartmentRepo, KnowledgeBaseRepo 同理改造。这些接口会在之后由 Spring Data MyBatis 或手动编写 Impl 来绑定。

---

### Phase 4: 鉴权与拦截器

#### Task 4.1: 创建 AuthInterceptor

**文件**: `demo/src/main/java/com/openSupports/demo/application/interceptor/AuthInterceptor.java`

```java
package com.openSupports.demo.application.interceptor;

import com.openSupports.demo.Domain.repo.AccountRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AccountRepo accountRepo;

    public AuthInterceptor(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            sendUnauthorized(response, "会话已过期，请重新登录");
            return false;
        }

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            sendUnauthorized(response, "未登录");
            return false;
        }

        var account = accountRepo.findById(userId);
        if (account == null || !account.isEnabled()) {
            sendForbidden(response, "账户已被停用");
            return false;
        }

        // Store user info for downstream use
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUserRole", account.getLevel() != null ? "STAFF_" + account.getLevel() : "USER");
        request.setAttribute("currentUserDeptId", account.getDepartmentId());
        request.setAttribute("currentUserEmail", account.getEmail());
        return true;
    }

    private void sendUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"" + msg + "\"}");
    }

    private void sendForbidden(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"" + msg + "\"}");
    }
}
```

#### Task 4.2: 创建 WebMvcConfig

**文件**: `demo/src/main/java/com/openSupports/demo/application/config/WebMvcConfig.java`

```java
package com.openSupports.demo.application.config;

import com.openSupports.demo.application.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/signup",
                        "/api/v1/auth/signin",
                        "/api/v1/kb/folders",          // Public KB browse
                        "/api/v1/kb/articles/**"        // Public article read
                );
    }
}
```

---

### Phase 5: Controller 层修复

现有 Controller 有严重问题：无 Spring 注解、方法返回 void 但不发响应、类型引用错误（TicketBrief vs TicketListItemView）。统一重写。

#### Task 5.1: 重写 AccountController

**文件**: `demo/src/main/java/com/openSupports/demo/api/controller/AccountController.java`

```java
package com.openSupports.demo.api.controller;

import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.user.*;
import com.openSupports.demo.api.dto.staff.{SearchUsersCmd, SearchStaffCmd, CreateStaffCmd, CreateUserCmd, ChangeStaffLevelCmd, ChangeStaffDeptCmd, ChangeStaffPasswordCmd, ChangeStaffStateCmd, ChangeUserPasswordCmd, ChangeUserStateCmd, UserOverviewView, DetailedUserView, StaffOverviewView, DetailedStaffView};
import com.openSupports.demo.api.dto.common.PageResult;
import com.openSupports.demo.api.dto.ticket.TicketListItemView;
import com.openSupports.demo.application.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/auth/signup")
    public TokenResp signup(@Valid @RequestBody UserSignupCmd cmd) {
        return accountService.signup(cmd);
    }

    @PostMapping("/auth/signin")
    public TokenResp signin(@Valid @RequestBody UserSigninCmd cmd, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> attrs = accountService.signin(cmd.getEmail(), cmd.getPassword(), session, cmd.getRememberMe());
        return new TokenResp(
            (String) attrs.get("token"),
            attrs.get("userId").toString(),
            (Long) attrs.get("staffLevel"),
            (String) attrs.get("departmentId"),
            (Boolean) attrs.get("requireRememberCookie")
        );
    }

    @PutMapping("/account/email")
    public OperationResult changeEmail(@Valid @RequestBody ChangeEmailCmd cmd, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        accountService.changeEmail(userId, cmd.getNewEmail());
        return new OperationResult(true, "邮箱已更新");
    }

    @PutMapping("/account/password")
    public OperationResult changePassword(@Valid @RequestBody ChangePasswordCmd cmd, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        accountService.changePassword(userId, cmd.getCurrentPassword(), cmd.getNewPassword());
        return new OperationResult(true, "密码已更新");
    }

    // --- Admin endpoints ---

    @GetMapping("/admin/users")
    public PageResult<UserOverviewView> searchUsers(SearchUsersCmd cmd, HttpServletRequest request) {
        return accountService.searchUsers(cmd);
    }

    @GetMapping("/admin/users/{id}")
    public DetailedUserView getUserDetail(@PathVariable Long id) {
        return accountService.getUserDetail(id);
    }

    @PostMapping("/admin/staff")
    public OperationResult createStaff(@Valid @RequestBody CreateStaffCmd cmd) {
        accountService.createStaff(cmd);
        return new OperationResult(true, "员工账号已创建");
    }

    @PutMapping("/admin/staff/{id}/level")
    public OperationResult changeStaffLevel(@PathVariable Long id, @Valid @RequestBody ChangeStaffLevelCmd cmd) {
        accountService.changeStaffLevel(id, cmd.getLevel());
        return new OperationResult(true, "级别已更新");
    }

    @PutMapping("/admin/staff/{id}/department")
    public OperationResult changeStaffDept(@PathVariable Long id, @Valid @RequestBody ChangeStaffDeptCmd cmd) {
        accountService.changeStaffDept(id, cmd.getTargetDepartmentId());
        return new OperationResult(true, "部门已变更");
    }

    @PutMapping("/admin/staff/{id}/password")
    public OperationResult changeStaffPassword(@PathVariable Long id, @Valid @RequestBody ChangeStaffPasswordCmd cmd) {
        accountService.changeStaffPassword(id, cmd.getNewPassword());
        return new OperationResult(true, "密码已重置");
    }

    @PutMapping("/admin/staff/{id}/state")
    public OperationResult changeStaffState(@PathVariable Long id, @Valid @RequestBody ChangeStaffStateCmd cmd) {
        accountService.changeStaffState(id, cmd.getEnabled());
        return new OperationResult(true, "状态已更新");
    }

    @DeleteMapping("/admin/staff/{id}")
    public OperationResult deleteStaff(@PathVariable Long id) {
        accountService.deleteStaff(id);
        return new OperationResult(true, "员工账号已删除");
    }

    @GetMapping("/admin/staff")
    public PageResult<StaffOverviewView> searchStaff(SearchStaffCmd cmd) {
        return accountService.searchStaff(cmd);
    }

    @GetMapping("/admin/staff/{id}")
    public DetailedStaffView getStaffDetail(@PathVariable Long id) {
        return accountService.getStaffDetail(id);
    }

    @PostMapping("/admin/users")
    public OperationResult createUser(@Valid @RequestBody CreateUserCmd cmd) {
        accountService.createUser(cmd);
        return new OperationResult(true, "用户账号已创建");
    }

    @PutMapping("/admin/users/{id}/password")
    public OperationResult changeUserPassword(@PathVariable Long id, @Valid @RequestBody ChangeUserPasswordCmd cmd) {
        accountService.changeUserPassword(id, cmd.getNewPassword());
        return new OperationResult(true, "密码已重置");
    }

    @PutMapping("/admin/users/{id}/state")
    public OperationResult changeUserState(@PathVariable Long id, @Valid @RequestBody ChangeUserStateCmd cmd) {
        accountService.changeUserState(id, cmd.getEnabled());
        return new OperationResult(true, "状态已更新");
    }

    @DeleteMapping("/admin/users/{id}")
    public OperationResult deleteUser(@PathVariable Long id) {
        accountService.deleteUser(id);
        return new OperationResult(true, "用户账号已删除");
    }
}
```

#### Task 5.2: 重写 TicketController

**文件**: `demo/src/main/java/com/openSupports/demo/api/controller/TicketController.java`

参考现有 Skeleton 但补全注解和方法体委托给 Service。

Key endpoints:
- `POST /api/v1/tickets` → createTicket
- `GET /api/v1/tickets/me-assigned` → getAssignedTickets  
- `GET /api/v1/tickets/me-sent` → getSentTickets
- `GET /api/v1/tickets/new` → getNewTickets (unassigned, for staff)
- `GET /api/v1/tickets/all` → getAllDepartmentTickets
- `GET /api/v1/tickets/search` → searchByTitle
- `GET /api/v1/tickets/{id}` → getTicketDetail
- `POST /api/v1/tickets/{id}/comments` → addComment
- `PUT /api/v1/tickets/{id}/title` → editTitle
- `PUT /api/v1/tickets/comments/{commentId}` → editComment
- `POST /api/v1/tickets/{id}/tags` → addTag
- `DELETE /api/v1/tickets/{id}/tags/{tagId}` → removeTag
- `PUT /api/v1/tickets/{id}/assign` → assignTicket
- `PUT /api/v1/tickets/{id}/unassign` → unassignTicket
- `PUT /api/v1/tickets/{id}/department` → changeDept
- `PUT /api/v1/tickets/{id}/close` → closeTicket
- `PUT /api/v1/tickets/{id}/reopen` → reopenTicket
- `DELETE /api/v1/tickets/{id}` → deleteTicket
- `GET /api/v1/tickets/advanced-search` → advancedSearch

#### Task 5.3: 重写 KnowledgeBaseController

**文件**: `demo/src/main/java/com/openSupports/demo/api/controller/KnowledgeBaseController.java`

Endpoints:
- `GET /api/v1/kb/folders` → getFolders (public for visitors)
- `GET /api/v1/kb/articles/{id}` → getArticleDetail (public)
- `POST /api/v1/kb/folders` → createFolder (L2+)
- `PUT /api/v1/kb/folders/{id}` → updateFolder (L2+)
- `DELETE /api/v1/kb/folders/{id}` → deleteFolder (L2+, articles go to NULL)
- `PUT /api/v1/kb/folders/order` → updateFolderOrder (drag-drop)
- `PUT /api/v1/kb/folders/{id}/state` → changeFolderState (private toggle)
- `POST /api/v1/kb/articles` → createArticle (L2+)
- `PUT /api/v1/kb/articles/{id}` → updateArticle (L2+)
- `DELETE /api/v1/kb/articles/{id}` → deleteArticle (L2+)

#### Task 5.4: 重写 DepartmentController

**文件**: `demo/src/main/java/com/openSupports/demo/api/controller/DepartmentController.java`

Endpoints (L3 only):
- `GET /api/v1/departments` → listDepartments
- `POST /api/v1/departments` → createDepartment
- `PUT /api/v1/departments/{id}` → updateDepartmentName
- `DELETE /api/v1/departments/{id}` → deleteDepartment
- `POST /api/v1/departments/{id}/migrate-tickets` → migrateTickets
- `PUT /api/v1/departments/{id}/migrate-staff` → migrateStaff

---

### Phase 6: Application Service 层完善

#### Task 6.1: 完善 AccountService

**文件**: `demo/src/main/java/com/openSupports/demo/application/service/AccountService.java`

实现完整的业务流程，包括密码哈希（BCrypt）、会话创建、事务管理等。

关键方法清单：
- `signup(UserSignupCmd)` → create account → return TokenResp
- `signin(email, password, session, rememberMe)` → verify → create session → return auth info
- `changeEmail(userId, newEmail)` → check uniqueness → update
- `changePassword(userId, oldPw, newPw)` → verify old → update
- `searchUsers(cmd)` → pagination → build PageResult<UserOverviewView>
- `getUserDetail(id)` → find + count tickets → build DetailedUserView
- `createStaff(CreateStaffCmd)` → check dup → create staff account
- `changeStaffLevel(id, level)` → update level
- `changeStaffDept(id, deptId)` → check dept exists → transfer → unassign all tickets
- `changeStaffPassword(id, newPw)` → reset password
- `changeStaffState(id, enabled)` → enable/disable + unassign tickets on disable
- `deleteStaff(id)` → check not self → unassign all → delete
- `createUser(CreateUserCmd)` → check dup → create user
- `changeUserPassword(id, newPw)` → reset
- `changeUserState(id, enabled)` → toggle
- `deleteUser(id)` → check not self → close/unassign tickets → delete

#### Task 6.2: 完善 TicketService

参考现有骨架但修复 bug（TicketBrief → TicketListItemView, 方法签名补全等）。

#### Task 6.3: 完善 KnowledgeBaseService

#### Task 6.4: 完善 TicketDomainService

实现具体的权限校验逻辑：
- `canCloseTicket(ticket, account)` → users can close own; staff can close assigned/dept
- `canReOpenTicket(ticket, account)` → same permissions as close
- `canDeleteTicket(ticket, account)` → L3 only

---

### Phase 7: Handler & Domain Event Publishing

#### Task 7.1: 完善 TicketHandler

修复现有的粗糙实现，改为精确查找而非全表扫描：
- `onTagDeleted(TagDeleted)` → find affected tickets by tag name, remove tags
- `onStaffDeleted(StaffDeleted)` → unassign and close related tickets for specific staff
- `onStaffDisabled(StaffDisabled)` → unassign related tickets only for disabled staff

#### Task 7.2: 完善 AccountHandler

添加对应事件监听：
- `onUserRegistered(UserRegistered)` → welcome notification placeholder
- `onStaffCreated(StaffCreated)` → setup defaults

#### Task 7.3: 实现 AOP Event Publisher

由于项目不用 Spring AOP 做 domain event 发布，用一个简单的 ApplicationEventPublisher 包装：

**文件**: `demo/src/main/java/com/openSupports/demo/infra/event/DomainEventPublisher.java`

```java
package com.openSupports.demo.infra.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.openSupports.demo.Domain.event.Event;

@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher publisher;

    public DomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Event event) {
        publisher.publishEvent(event);
    }
}
```

然后在 Domain 层的每个 Command Method 后调用 `eventPublisher.publish(...)` 来触发事件。

---

### Phase 8: 工具类与安全

#### Task 8.1: 创建密码工具

**文件**: `demo/src/main/java/com/openSupports/demo/infra/util/PasswordUtil.java`

```java
package com.openSupports.demo.infra.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

public final class PasswordUtil {
    private PasswordUtil() {}

    public static String hash(String plainPassword, String salt) {
        return BCrypt.hashpw(plainPassword + salt, BCrypt.gensalt());
    }

    public static boolean verify(String plainPassword, String salt, String hashed) {
        return BCrypt.checkpw(plainPassword + salt, hashed);
    }
}
```

> 需在 pom.xml 添加 `spring-security-crypto` 依赖（BCrypt 不在 starter-web 内）。

#### Task 8.2: 创建 TicketCode 生成器

**文件**: `demo/src/main/java/com/openSupports/demo/infra/util/TicketCodeGenerator.java`

生成格式如 `TK-20260919-XXXXX` 的唯一工单编号。

#### Task 8.3: 创建上传文件服务

**文件**: `demo/src/main/java/com/openSupports/demo/infra/util/FileUploadService.java`

支持最大 10MB/文件，保存到 app.upload-dir 配置目录，返回 Attachment 元数据。

---

### Phase 9: 数据库初始化 & 种子数据

#### Task 9.1: 创建 SqlScriptInitializer

**文件**: `demo/src/main/java/com/openSupports/demo/infra/init/SqlScriptInitializer.java`

在 Spring Boot 启动时自动执行 `schema.sql`，如果没有已存在的数据库文件。同时插入默认部门记录（Software Support, is_default=true）。

---

### Phase 10: 测试

#### Task 10.1: 创建单元测试 — AccountServiceTest

使用 H2 内存数据库 + MyBatis Test，覆盖：
- signup success / email duplicate
- signin correct password / wrong password
- changeEmail valid / duplicate
- changePassword valid / wrong current

#### Task 10.2: 创建集成测试 — TicketControllerTest

模拟 HTTP 请求，覆盖：
- create ticket flow (login → create → verify code)
- assign/unassign
- close/reopen
- permission enforcement

#### Task 10.3: 端到端测试脚本

一个 curl 脚本 `tests/e2e.sh`，依次执行注册→登录→建单→评论→分配→关闭→查看完整生命周期。

---

## Tests / Validation Summary

### Build verification commands (run at each phase boundary):
```bash
cd demo && mvn compile -q && echo "✅ Compile OK" || echo "❌ Build failed"
cd demo && mvn test -q && echo "✅ All tests passed" || echo "❌ Tests failed"
```

### Coverage checklist against BA.md:

| UC编号 | 名称 | 实现阶段 | 验证方式 |
|--------|------|----------|---------|
| UC-01 | 用户注册 | Phase 5 + 6.1 | Unit Test |
| UC-02 | 账户登录 | Phase 5 + 6.1 | Integration Test |
| UC-03 | 个人资料管理 | Phase 5 + 6.1 | Unit Test |
| UC-04 | 创建工单 | Phase 5 + 6.2 | E2E Script |
| UC-05 | 工单粗略视图(5种) | Phase 5 + 6.2 | Integration Test |
| UC-06 | 工单具体视图 | Phase 5 + 6.2 | Integration Test |
| UC-07 | 回复工单 | Phase 5 + 6.2 | E2E Script |
| UC-08 | 编辑工单 | Phase 5 + 6.2 | Unit Test |
| UC-09 | 工单标签 | Phase 5 + 6.2 | Unit Test |
| UC-10 | 工单管理 | Phase 5 + 6.2 | E2E Script |
| UC-11 | 高级搜索 | Phase 5 + 6.2 | Integration Test |
| UC-12 | 用户搜索与治理 | Phase 5 + 6.1 | Integration Test |
| UC-13 | Staff搜索 | Phase 5 + 6.1 | Integration Test |
| UC-14 | 员工管理 | Phase 5 + 6.1 | E2E Script |
| UC-15 | 用户管理 | Phase 5 + 6.1 | E2E Script |
| UC-16 | 知识库浏览/主题管理 | Phase 5 + 6.3 | E2E Script |
| UC-17 | 文章管理 | Phase 5 + 6.3 | E2E Script |
| UC-18 | 部门管理 | Phase 5 + 6.4 | E2E Script |

**所有 18 个 UC 均已纳入计划。**

---

## Risks, Tradeoffs, and Open Questions

### 风险

1. **Spring Boot 4.0.8 兼容性问题**: 如果 Spring Boot 4.0 尚未正式发布，某些依赖可能不可用。需要确认 Maven Central 上是否存在 `spring-boot-starter-parent:4.0.8`。如果不存在，可能需要回退到 3.2.x。
   - *缓解*: 先尝试 `mvn dependency:tree` 检查是否能 resolve，如果失败则调整版本号。

2. **SQLite 连接池**: HikariCP 对 SQLite 的连接池支持有限，并发写入可能遇到 `SQLITE_BUSY` 错误。
   - *缓解*: 设置合理的 pool size（max 5），在 TicketDomainService 中添加重试逻辑处理 BUSY 错误。

3. **MySQL connector 残留**: pom.xml 中的 `mysql-connector-j` 必须完全移除，否则会传递引入 MySQL 驱动导致 classpath 冲突。

4. **Session JDBC vs SQLite**: spring-session-jdbc 需要特定格式的 session 表（已在 schema.sql 中提供），但需要确认 SQLite 兼容性。

### 取舍

- **不使用 Spring Security**: 使用自定义 AuthInterceptor 而非完整的安全框架，降低复杂度。
- **不使用 MyBatis Plus**: 纯手写 XML Mapper，保持对 DDD 架构的清晰理解。
- **不使用 Swagger/OpenAPI**: 快速 MVP 优先，API 契约已有设计文档。
- **附件存储走文件系统而非数据库 BLOB**: 更实用的选择，适合 small-medium 规模。
- **密码哈希用 BCrypt (spring-security-crypto)**: 不需要完整 security-framework，只抽取 crypto 模块。

### 待确认问题

1. **密码加密算法**: 原始 BA 没指定，本项目选用 BCrypt（Spring 内置支持，安全且成熟）。
2. **ticket code 生成规则**: 暂定 `TK-{yyyyMMdd}-{5位随机数}`，如需其他规则请告知。
3. **是否需要 JWT 替代 Session**: 目前方案使用 Spring Session JDBC，如果需要微服务扩展可后续迁移到 JWT。
4. **CSRF 保护**: BA.md 中提到 CSRF 参数但 API 场景下通常用 Token 而非 CSRF。暂时不做，仅在需要浏览器表单提交时启用。
5. **MultipartFile 上传路径**: 当前设计在 DTO 中包含附件字段，但 Multipart 请求不能直接用 @RequestBody。建议在 Controller 层单独处理 multipart，附件作为独立资源存储在文件系统。
