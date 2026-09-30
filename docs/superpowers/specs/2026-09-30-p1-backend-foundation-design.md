# P0+P1 设计：基线保护与后端底座现代化

- 日期：2026-09-30
- 项目：`exam_system_online`（后端）+ `exam-system-web-backup`（前端，本阶段不改）
- 状态：待评审
- 关联路线：全量重构方案 **A（后端优先纵切）**，见「总体路线」

## 1. 背景

`exam_system_online` 是一个在线考试/学习平台后端（单模块 Spring Boot 3.0.5 / Java 17 /
MyBatis-Plus 3.5.3.2，118 个类，约 7800 行），配套前端为 Vue 3 + Vite 4 的
`exam-system-web-backup`（43 个视图）。

用户目标是**从架构、DB、SQL IO、版本全量升级**，定位为学习/作品集项目（本机运行，允许破坏性
变更和改表结构）。经评估，该目标横跨 8 个相对独立的子项目，不适合单次交付，故拆分为分阶段
路线（见 §3），本文档只覆盖第一阶段 **P0 基线保护 + P1 后端底座现代化**。

### 现状关键问题（本阶段要解决的）

- `application.yml` 内含**明文 Moonshot API Key**（`sk-...`），等同泄露。
- 11 个 Controller 挂 `@CrossOrigin(origins = "*")`，CORS 全放开。
- Hikari 配置反向优化：`max-lifetime=60s`、`idle-timeout=60s`、`maximum-pool-size=8`、
  `connection-timeout=60s`，导致连接频繁重建。
- 6 个类用 `@Log4j2`、3 个类用 `@Slf4j`，但 POM **未引入 log4j**，日志依赖启动器桥接，风格不一致。
- `spring.main.allow-circular-references: true` 说明存在循环依赖（P4 处理）。
- 依赖版本混乱且偏旧：`mybatis-plus-boot-starter`（Boot3 应使用 `mybatis-plus-spring-boot3-starter`）、
  冗余的 `com.alibaba:fastjson:2.0.25` 桥接包、古代 `lombok-maven-plugin:1.18.20.0`、重复声明
  的 `project.build.sourceEncoding`。
- `Banner.java` 使用 fastjson **1.x** 注解 `com.alibaba.fastjson.annotation.JSONField`，
  与其余类使用的 `com.alibaba.fastjson2` 混用。

## 2. 范围

本阶段只做 **P0 + P1**：

- P0：建立可回退的绿色基线（git 标签/分支、schema dump 存档、冒烟脚本）。
- P1：后端依赖与版本现代化、配置与密钥外置、运行时加固、统一响应/异常/校验。

**明确不做**：认证鉴权（P2）、schema 重设计（P3）、分层重构（P4）、补齐缺失业务（P5）、
前端工程化与 UI（P6/P7）。

## 3. 总体路线（方案 A，供上下文）

| 阶段 | 内容 |
|---|---|
| **P0 基线保护** | git 分支/标签、schema dump、冒烟脚本 |
| **P1 后端底座现代化** | 版本升级、配置外置、CORS/连接池/日志、统一响应异常 |
| P2 认证鉴权 | Spring Security + JWT + 角色 |
| P3 数据层重建 | schema 重设计 + Flyway + 数据迁移 + SQL IO 优化 |
| P4 深度分层 + 核心域重构 | feature 分包、DTO/VO 分离、MapStruct、AI 判卷异步化 |
| P5 补齐缺失业务 | 企业真题、模拟面试、邀请码、积分、企业、用户投稿/待审 |
| P6 前端工程化 + TS | Vite 7、TS、Pinia、统一 request、env、lint |
| P7 前端 UI 重做 | 设计系统/主题/布局/页面重写 |
| P8 测试/文档/交付 | 单测/集成测试、README、OpenAPI、docker-compose |

## 4. P0 详细设计

### 4.1 Git 保护

- 在现有 `main`（单 commit）打标签 `pre-refactor-baseline`。
- 新建并切换到分支 `refactor/p1-foundation`，本阶段所有改动落在此分支。

### 4.2 Schema 基线存档

- `mysqldump --no-data --routines --triggers online_exam` →
  `docs/db/baseline/online_exam_schema_baseline.sql`。
- `mysqldump --no-create-info --complete-insert online_exam`（或按表导出）→
  `docs/db/baseline/online_exam_data_baseline.sql`（P3 数据迁移的输入）。
- 两者**纳入 git**（数据量小，约 700 行），作为不可变基线。

### 4.3 冒烟脚本

- 新增 `scripts/smoke.sh`：构建 → 后台启动应用 → 轮询健康 → 逐个请求并断言
  `code == 200` → 关闭应用，失败即非零退出。
- 覆盖接口（各域关键 GET）：
  - `/api/questions/list`、`/api/papers/list`、`/api/categories`、`/api/categories/tree`
  - `/api/videos`、`/api/videos/popular`、`/api/banners/active`、`/api/notices/active`
  - `/api/stats/overview`、`/api/video-categories/tree`
- 启动端口 8090；脚本内可用 `--skip-build` 复用已有产物。

## 5. P1 详细设计

### 5.1 依赖与版本（P1.1）

| 项 | 现状 | 目标 |
|---|---|---|
| Spring Boot parent | 3.0.5 | **3.5.3**（本地 m2 已备） |
| Java | 17 | **21**（本机 `ms-21.0.10`） |
| MyBatis-Plus | `mybatis-plus-boot-starter` 3.5.3.2 | `mybatis-plus-spring-boot3-starter` **3.5.9** |
| fastjson | `com.alibaba:fastjson` 2.0.25（桥接） | `com.alibaba.fastjson2:fastjson2` **2.0.65**（直连） |
| Knife4j | 4.4.0 | **4.5.0** |
| springdoc | 隐式 | 显式 **2.8.6**（与 Boot 3.5 对齐） |
| Redisson | 3.24.3 | **3.44.0** |
| MinIO | 8.5.7 | **8.5.17** |
| POI | 5.2.4 | **5.4.1** |
| Hutool | 5.8.3 | **5.8.36** |
| Lombok | 手动 + 古代 delombok 插件 | 由 Boot parent 管理，**删除 `lombok-maven-plugin`** |

其他构建清理：

- 删除重复的 `project.build.sourceEncoding`；`maven.compiler.source/target` → `maven.compiler.release=21`。
- 父 POM 已管理版本的依赖去掉显式 `<version>`（jackson-databind、mysql-connector-j、
  spring-boot-starter-* 等）。
- `Banner.java` 的 `com.alibaba.fastjson.annotation.JSONField` → fastjson2 注解
  （`com.alibaba.fastjson2.annotation.JSONField`）或按需移除。
- `spring-boot-starter-webflux` 保留（Kimi 调用使用 WebClient）。

### 5.2 配置与密钥外置（P1.2）

- 新增 `application-local.yml`，加入 `.gitignore`；`application.yml` 中密钥改为
  `${KIMI_API_KEY:}` 占位并从本地文件/环境注入。
- 移除 `application.yml` 明文 `api-key`；提交后 `git grep -n 'sk-' -- 'src/**' 'application.yml' 'pom.xml'` 必须为空（文档/模板占位不算）。
- Profile 约定：`dev`（默认，指向 localhost 中间件）、`local`（个人覆盖）、`prod`。
- **新增** `spring.config.import: optional:classpath:application-local.yml`（现状无此配置）。
- `.gitignore` 增加 `application-local.yml`（及 `application-local*.yml`）。

### 5.3 运行时加固（P1.3）

**Hikari（application.yml）**

| 参数 | 现状 | 目标 |
|---|---|---|
| `connection-timeout` | 60000 | 30000 |
| `idle-timeout` | 60000 | 600000 |
| `max-lifetime` | 60000 | 1800000 |
| `maximum-pool-size` | 8 | 20 |
| `minimum-idle` | 8 | 5 |
| `connection-test-query` | SELECT 1 | 移除（依赖 JDBC4 `isValid`） |

**CORS**

- 删除 11 个 Controller 的 `@CrossOrigin`。
- 新增 `WebMvcConfig#addCorsMappings`，允许来源可配置
  （默认 `http://localhost:3001`，通过 `app.cors.allowed-origins` 覆盖）。

**日志**

- 统一为 `@Slf4j`（6 处 `@Log4j2` → `@Slf4j`）。
- `mybatis-plus.configuration.log-impl` 从 `StdOutImpl` 改为按 profile 控制（dev 可开，prod 关）。

**Actuator**

- 仅暴露 `health,info`；`management.endpoints.web.exposure.include=health,info`。

### 5.4 统一响应 / 异常 / 校验（P1.4）

- 新增 `ErrorCode` 枚举（成功 200、参数错误 400、未登录 401、无权限 403、未找到 404、
  业务失败 500 等）。
- 新增 `BizException extends RuntimeException`（携带 `ErrorCode`）。
- `GlobalExceptionHandler`（`@RestControllerAdvice`）统一处理：
  业务异常、`MethodArgumentNotValidException`/`BindException`（参数校验）、兜底 `Exception`；
  返回 `Result.error(...)`，并记录日志。
- 关键 VO 增加 Jakarta 校验注解，Controller 入参加 `@Valid`。
- `Result` 保留现有形状（`code/message/data`），前端无需改动。

## 6. 关键决策记录

| 决策 | 选择 | 理由 |
|---|---|---|
| 后端端口 | **8090**（2026-09-30 变更，原定 8080） | 8080 被本机 `qcsz/4T-open-user-bff` 占用；P6 前端改用环境变量 |
| Spring Boot | **3.5.3** | 本地 m2 已有，无需联网，稳 |
| 密钥存放 | **`application-local.yml`**（gitignore） | 用户选择本地文件方案 |
| 架构风格 | 单模块深度分层 | P4 执行，本阶段不改包结构 |
| 迁移工具 | Flyway（P3 引入） | 本阶段只 dump 基线 |

## 7. 验收标准

1. `mvn clean package -DskipTests` 在 Java 21 下通过；`java -jar` 可启动于 8090。
2. `scripts/smoke.sh` 全绿（上述接口均 `code == 200`）。
3. 前端**本阶段不做修改**；因端口改为 8090，前端 `request.js` 仍指向 8080，故 P1 结束时前端无法直接连通（将在 P6 改为环境变量）。接口契约本身保持不变。
4. `git grep -n 'sk-' -- 'src/**' 'application.yml' 'pom.xml'` 无命中；`application-local.yml` 已被 gitignore。
5. 仓库内不再有 `@CrossOrigin`、`@Log4j2`；Hikari 参数为 §5.3 目标值。
6. 新增单测：`ResultTest`、`GlobalExceptionHandlerTest`；`mvn test -Dtest=...` 通过。
7. `git tag pre-refactor-baseline` 存在，工作落在 `refactor/p1-foundation` 分支。

## 8. 风险与回退

| 风险 | 缓解 |
|---|---|
| Boot 3.0.5→3.5.3 导致 Knife4j/springdoc 不兼容 | 显式锁定 knife4j 4.5.0 + springdoc 2.8.6；验收点校验 `/doc.html` 与 `/v3/api-docs` |
| MyBatis-Plus 坐标切换导致 mapper 扫描/分页失效 | 校验分页接口（list 类）返回正确；检查 `MybatisPlusConfig` 分页拦截器 |
| fastjson2 直连后 `Banner` 注解编译失败 | P1.1 内同步迁移注解，编译即验证 |
| Java 21 编译/运行差异 | 冒烟脚本覆盖启动与核心接口 |
| 改动混入业务语义导致前端报错 | 本阶段不改接口契约、不改前端；`Result` 形状不变 |
| 任何阶段失败 | 回到 `pre-refactor-baseline` 标签 |

## 9. 交付物清单

- 代码：POM、`application*.yml`、`.gitignore`、`WebMvcConfig`、`ErrorCode`、`BizException`、
  `GlobalExceptionHandler`、`Banner` 注解、6 处日志注解、Hikari 配置。
- 脚本/数据：`scripts/smoke.sh`、`docs/db/baseline/*.sql`。
- 测试：`ResultTest`、`GlobalExceptionHandlerTest`。
- 文档：本 spec；P1 完成报告（写入 `docs/superpowers/`）。
