# 智能考试/学习平台 — 总体开发计划（Master Development Plan）

> 本文档是**唯一的总体开发计划**，融合了已完成阶段成果与后续所有阶段的详细方案，
> 目标是让**任意工具/开发者无需回看历史对话即可接手继续开发**。
>
> - 后端仓库：`/Users/qyk9527/ideaProject/exam_system_online`（Spring Boot 单体）
> - 前端仓库：`/Users/qyk9527/webstormProject/exam-system-web-backup`（Vue 3 + Vite，包名 `smart-learning-platform-web`）
> - 数据库：本机 MySQL `localhost:3306`，库 `online_exam`（root/root123456）
> - 最近更新：2026-09-30（P1 完成后）

---

## 0. 如何使用本文档（给接手的工具/人）

1. 先读 §1 项目概览 + §2 现状盘点 + §3 目标架构，建立全局认知。
2. 读 §4 关键决策与 §5 阶段路线总览，了解进度（P0/P1 已完成）。
3. 从 §6 起按阶段推进；每个阶段都有「目标 / 范围 / 关键设计 / 数据变更 / 任务分解 / 验收 / 风险」。
4. 每进入一个新阶段，**建议先为其单独走一次「设计 → 计划 → 实施 → 评审」循环**（本仓库 P1 的做法见 §10 工程约定）。
5. 工程命令、端口、分支规范见 §10；遗留缺陷清单见 §9。

---

## 1. 项目概览

一个「在线考试 + 技术学习」平台：题库/组卷/在线考试/AI 判卷/成绩排行，外加技术短视频、
企业面试真题、模拟面试、邀请码、积分等学习社区功能（部分前端页面已存在但后端未实现）。

- 定位：学习/练手/作品集项目（本机运行，允许破坏性变更与改表结构）。
- 总体策略：**【方案 A】后端优先纵切** —— 先把后端底座、数据、架构、业务补齐，再做前端 TS+UI 重做。
- 当前进度：**P0、P1 已完成并合并到 `main`**；P2–P8 待开发。

### 1.1 技术栈现状（P1 之后）

| 层 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.5.3 |
| ORM | MyBatis-Plus | 3.5.9（`mybatis-plus-spring-boot3-starter` + `mybatis-plus-jsqlparser`） |
| DB | MySQL | 本机 9.6（`online_exam`，utf8mb4_0900_ai_ci） |
| 缓存/锁 | Redis + Redisson | 6379 / Redisson 3.44.0 |
| 对象存储 | MinIO | 8.5.17（`localhost:9000`，minioadmin/minioadmin，bucket `exam-system-bucket`） |
| AI | Moonshot Kimi（WebClient） | `moonshot-v1-32k` |
| Excel | Apache POI | 5.4.1 |
| JSON | fastjson2 | 2.0.65 |
| 工具 | Hutool | 5.8.36 |
| API 文档 | Knife4j + springdoc | 4.5.0 / 2.8.6 |
| 前端 | Vue 3 + Vite + Element Plus + Pinia | Vue 3.3 / Vite 4 / Element Plus 2.3 |
| 构建 | Maven（**本机未装 mvn，用 IDEA 内置**） | — |

### 1.2 运行前提

- MySQL 本机直装；Redis + MinIO 走 Docker（用户约定）。
- 后端端口 **8090**（原 8080 因被另一项目 `qcsz/4T-open-user-bff` 占用而改）。
- 前端 dev 端口 3001（`vite.config.ts` 读 `VITE_DEV_PORT`）。前端 `request.ts` 的 baseURL 已改为读 `VITE_API_BASE_URL`，`.env.development` 指向 **8090**（2026-10-01 P6 已完成，原先写死 8080 导致全站请求打不到后端）。

---

## 2. 现状盘点

### 2.1 后端结构（单模块）

```
src/main/java/com/joker/ai/exam/
├── ExamSystemOnlineApp.java        # 唯一入口
├── common/    Result, CacheConstants, TreeBuilder, TreeNode, ErrorCode, BizException
├── config/    GlobalExceptionHandler, KimiProperties, Knife4jConfiguration,
│              MinioConfig, MinioProperties, MybatisPlusConfig, RedisConfig,
│              WebClientConfiguration, WebMvcConfig
├── controller/ 14 个（Question/QuestionBatch/Paper/Exam/ExamRecord/Category/
│              Notice/Banner/Video/VideoAdmin/VideoCategory/File/Stats/User）
├── entity/    13 个（AnswerRecord/Banner/Category/ExamRecord/Notice/Paper/
│              PaperQuestion/Question/QuestionAnswer/QuestionChoice/User/
│              Video/VideoCategory/VideoLike/VideoViews）
├── mapper/    15 个（+ resources/mapper/*.xml，多为空壳）
├── service/   接口 + impl（有 5 个 impl 含 return null 空实现桩，见 §9）
├── utils/     ExcelUtil, IpUtils, RedisUtils(364 行自定义)
└── vo/        25 个 DTO/VO
```

- **已有 Spring Security + JWT**（P2，2026-10-01）：`auth/` 包 + `AuthController`；`users.password` 已 BCrypt、`role` 归一大写；`UserController.checkAdmin` 走真实角色判定。
- 遗留：`/api/exams/{id}` 无数据级 ownership 校验（P4）；`QuestionController`/`ExamController` 仍有 7 个 `Result.success(null)` 空桩（见 §9.5b）。
- 存在循环依赖：`application.yml` 里 `spring.main.allow-circular-references: true`（P4 消除）。
- Kimi 判卷是**请求线程内同步阻塞**调用（`InterruptedException` 透出），需 P4 异步化。

### 2.2 数据库现状（15 张表）

| 表 | 主键/关键列 | 备注 |
|---|---|---|
| `users` | id, username(uniq), password, real_name, role, status | 1 行（admin/admin123 明文） |
| `questions` | id, title, type, multi, category_id, difficulty, score, analysis | 75 行；**无 category_id/type 索引** |
| `question_choices` | id, question_id, content, is_correct, sort | 245 行；**无 question_id 索引** |
| `question_answers` | id, question_id, answer, keywords | 64 行；**无 question_id 索引** |
| `categories` | id, name, parent_id, sort | 17 行；**无 parent_id 索引** |
| `paper` | id, name, description, status, total_score, question_count, duration | 10 行；**无 status 索引** |
| `paper_question` | id, paper_id, question_id, score | 116 行；**无 paper_id/question_id 索引** |
| `exam_records` | id, exam_id, student_name, score, answers, start_time, end_time, status, window_switches | 10 行；**无 exam_id/status/student_name 索引** |
| `answer_record` | id, exam_record_id, question_id, user_answer, score, is_correct, ai_correction | 88 行；**仅主键，无任何索引** |
| `banners` | id, title, description, image_url, link_url, sort_order, is_active | 有 idx_sort_order/is_active |
| `notices` | id, title, content, type, priority, is_active | 有 idx_type/priority/is_active/create_time |
| `videos` | id, title, description, category_id, file_url, cover_url, duration, file_size, uploader_name, uploader_type, user_id, admin_id, status, audit_*, view_count, like_count, tags | 有多个索引 + FK category_id |
| `video_categories` | id, name, description, parent_id, sort_order, status | **无 is_deleted 列** |
| `video_likes` | id, video_id, user_ip, user_agent | uk_video_ip(video_id,user_ip) + FK |
| `video_views` | id, video_id, user_ip, user_agent, view_duration | idx video_id/created_at/user_ip + FK |

**结构不一致（P3 统一）**：
- 时间列两种命名混用：`create_time/update_time`（业务表）vs `created_at/updated_at`（视频表）。
- 逻辑删除 `is_deleted`：视频三表 + `video_categories` **缺失**。
- 外键约束仅视频域有；业务表之间无 FK。
- 上述多处**索引缺失**（N+1/慢查询风险点）。

### 2.3 前端结构

```
src/
├── main.js, App.vue
├── api/       exam.js, paper.js, video.js, videoCategory.js, interviewQuestion.js
├── utils/     request.js（axios，baseURL 写死 8080，无 token 注入）
├── router/    index.js（无登录路由/无鉴权守卫，仅标题 + 考试结果返回拦截）
├── components/ CategoryTree.vue, QuestionList.vue, VideoSubmitForm.vue
└── views/     43 个页面（见 §11.2）
```

### 2.4 前后端功能缺口（关键！）

但后端**完全没有**以下接口，而前端有对应页面/API：

| 前端模块 | 前端 API 前缀 | 后端现状 |
|---|---|---|
| 企业面试真题 | `/api/interview-questions/**` | **无** |
| 模拟面试 | `/api/mock-interview/**`、`/interview/**` | **无** |
| 邀请码 | `/interview/codes/**` | **无** |
| 用户积分 | `/api/user-interview-credits/**`、`/interview/credits/**` | **无** |
| 企业管理 | （页面 `CompanyManage`） | **无** |
| 用户上传/待审 | `/api/user-contributions/**`、`PendingQuestionManage` | **无** |

→ 这些是 P5「补齐缺失业务」的主体。
→ 另外既有后端有 **5 个 Service 空实现桩**（Notice 等，见 §9）也归入 P5。

---

## 3. 目标架构

### 3.1 后端目标

- 保持**单模块**、按 **feature 分包 + 严格分层**：

```
com.joker.ai.exam
├── common/        Result, ErrorCode, BizException, PageResult, 常量/工具
├── config/        全局配置（Security, MybatisPlus, Web, Redis, Minio, Async, OpenAPI…）
├── <domain>/      按业务域分包（见下），每域内含：
│     controller/  REST 入口（只做参数/权限/编排）
│     service/     业务接口 + impl
│     mapper/      MyBatis-Plus Mapper
│     entity/      持久化实体
│     dto/         入参（含校验注解）
│     vo/          出参
│     convert/     MapStruct 转换器
└── infra/         基础设施（AI 客户端、存储、缓存封装等）
```

- 业务域建议：`user`、`question`、`paper`、`exam`、`video`、`banner`、`notice`、`category`、`stats`、`interview`（真题/模拟面试）、`code`（邀请码）、`credit`（积分）、`company`、`contribution`。
- 关键技术决策：
  - 认证鉴权：**Spring Security + JWT + RBAC**（角色 `STUDENT` / `ADMIN`）。
  - 对象转换：**MapStruct**（编译期生成，替代手写 set）。
  - AI 判卷：**异步化**（`@Async` + 有界线程池，或 Redis 队列），提交后立即返回、轮询/推送结果。
  - 参数校验：Jakarta Validation + 统一异常处理。
  - 统一响应：沿用 `Result{code,message,data}`（前端按 `code` 判断；HTTP 200 约定，见 §9.6）。
  - 数据迁移：**Flyway**（`V1__baseline.sql` 冻结现状，后续每个变更一个版本）。

### 3.2 前端目标

- Vite 7 + **TypeScript** + Vue 3 + Element Plus + Pinia。
- 统一 `request` 封装：baseURL 取环境变量、自动注入 JWT、401 刷新/跳登录、统一错误提示。
- 路由守卫 + 角色菜单；按域拆分 API 模块；Pinia 管理 user/exam 等状态。
- ESLint + Prettier；删除散落 `console.log`。
- UI 重做：设计系统（色板/间距/组件）、统一布局（前台/管理台）、核心页面重写。

---

## 4. 关键决策记录（已锁定）

| 决策 | 选择 | 时间/说明 |
|---|---|---|
| 推进策略 | 方案 A：后端优先纵切 | 2026-09-30 |
| 后端架构 | 单模块深度分层（非多模块） | 2026-09-30 |
| 前端范围 | TS 迁移 + UI 重做 | 2026-09-30 |
| 认证 | 完整 Spring Security + JWT + 角色 | 2026-09-30 |
| DB 策略 | 重设计表结构 + 数据迁移（保留数据） | 2026-09-30 |
| 后端版本 | Java 21 + Boot 3.5.3 | 2026-09-30（P1 已落地） |
| 密钥存放 | `application-local.yml`（gitignore）+ 模板 `.example` | 2026-09-30（P1 已落地） |
| 后端端口 | 8090 | 2026-09-30（原 8080 被占用） |
| 运行方式 | MySQL 本机；Redis/MinIO 走 Docker | 2026-09-30 |
| 补齐缺失功能 | 补齐后端（而非删除前端页面） | 2026-09-30 |

---

## 5. 阶段路线总览

| 阶段 | 内容 | 状态 |
|---|---|---|
| **P0** | 基线保护（git 标签/分支、DB dump、冒烟脚本） | ✅ 完成 |
| **P1** | 后端底座现代化（版本、密钥、Hikari/日志/Actuator、CORS、异常/校验） | ✅ 完成（已合并 main） |
| **P2** | 认证鉴权（Spring Security + JWT + 角色） | ✅ 完成（2026-10-01）：JWT access + Redis refresh 轮换、`/api/**` 默认需登录、学生/管理员分权实测通过，`mvn test` 18/18、冒烟扩到 14 项 PASS。遗留见 `docs/superpowers/2026-10-01-p2-auth-completion.md` §4 |
| **P3** | 数据层重建（schema 重设计 + Flyway + 数据迁移 + SQL IO 优化） | ⬜ 待做 |
| **P4** | 深度分层 + 核心域重构（feature 分包、DTO/VO 分离、MapStruct、异步判卷） | ⬜ 待做 |
| **P5** | 补齐缺失业务（企业真题、模拟面试、邀请码、积分、企业、投稿/待审 + 补空实现桩） | ⬜ 待做 |
| **P6** | 前端工程化 + TS（Vite 7、TS、Pinia、统一 request、env、lint） | ✅ 完成（2026-10-01）：41 视图全迁 TS，`npm run verify` 全绿，已用真实后端 + 浏览器逐项核验。详见前端 `docs/superpowers/2026-10-01-p6-completion.md` |
| **P7** | 前端 UI 重做（设计系统、布局、页面） | ⬜ 待做 |
| **P8** | 测试/文档/交付（单测/集成测试、README、OpenAPI、docker-compose、CI） | ⬜ 待做 |

> 依赖关系：P2 与 P3 可并行或先后；P4 建议在 P2/P3 之后（分层重构需鉴权与数据稳定）；
> P5 依赖 P4 的分层范式；P6 依赖 P2 的接口契约与 P5 的接口清单；P7 依赖 P6。

---

## 6. P0/P1 已完成（成果与位置）

### P0 基线保护
- git 标签 `pre-refactor-baseline`（回退点）。
- DB 基线：`docs/db/baseline/online_exam_schema_baseline.sql`（结构）、`online_exam_data_baseline.sql`（数据）。
- 冒烟脚本 `scripts/smoke.sh`（9 个接口断言，`--skip-build` 复用产物）。

### P1 后端底座现代化（19 个提交）
- 版本：Boot 3.5.3 / Java 21 / MyBatis-Plus 3.5.9 / fastjson2 2.0.65 / Knife4j 4.5.0 / springdoc 2.8.6 / Redisson 3.44.0 / MinIO 8.5.17 / POI 5.4.1 / Hutool 5.8.36。
- 密钥：`application-local.yml`（gitignore）+ `application-local.yml.example`；`spring.config.import: optional:file:./application-local.yml`。
- 运行时：Hikari 参数修正；Actuator 仅 health/info；MyBatis 日志默认静默（本地可覆盖）；CORS 白名单（`WebMvcConfig` + `app.cors.allowed-origins`）；日志统一 `@Slf4j`。
- 异常/校验：`ErrorCode`、`BizException`、重写 `GlobalExceptionHandler`（含 404/405/415/400），`@Valid` 生效。
- 测试：`ResultTest`(2) + `GlobalExceptionHandlerTest`(5) = 7/7；冒烟 9 OK PASS。
- 文档：`docs/superpowers/specs/2026-09-30-p1-backend-foundation-design.md`、`docs/superpowers/plans/2026-09-30-p1-backend-foundation.md`、`docs/superpowers/2026-09-30-p1-completion.md`。

### P1 遗留（详见 §9）
密钥轮换（历史泄露，必须人工）、空实现 Service 桩、`Result` 重载陷阱、循环依赖开关等。

---

## 7. 逐阶段详细计划（P2–P8）

> 每阶段的「任务分解」是**工作包级**（非逐行 TDD）；接手时建议再细化为该阶段独立的 plan。

### P2 — 认证鉴权

**目标**：真实登录 + JWT + 角色权限；前端登录页 + token 注入 + 路由守卫。

**范围**
- 引入 `spring-boot-starter-security` + `jjwt`（或 `nimbus-jose-jwt`）。
- 角色：`STUDENT`、`ADMIN`（可在 `users.role` 基础上扩展）。
- 密码：BCrypt 存储（迁移已有 `admin/admin123`）。
- 接口：
  - `POST /api/auth/login`（用户名+密码 → access/refresh token + 用户信息）
  - `POST /api/auth/register`（可选，学生注册）
  - `POST /api/auth/refresh`、`POST /api/auth/logout`
  - `GET /api/auth/me`
- 放行白名单（无需登录）：登录/注册/刷新、静态资源、Knife4j、前台只读接口（视频/轮播/公告/试卷列表）。
- 受保护：管理端全部写接口需 `ADMIN`；考试提交/成绩需登录（学生）。
- `UserController.checkAdmin` 用真实鉴权替换硬编码 true。

**数据变更（与 P3 协调）**
- `users` 增：`password`（BCrypt）、`email`/`phone`（可选）、`role` 规范化枚举、`last_login_at`。
- 可选：`user_tokens`/黑名单表（Redis 存 refresh token 更佳）。

**任务分解**
1. 加依赖 + `SecurityConfig`（SecurityFilterChain、密码编码器、无状态会话、白名单）。
2. `JwtService`（签发/解析/校验、过期、刷新）。
3. `JwtAuthenticationFilter` + `UserDetailsService`。
4. `AuthController` + `AuthService`（登录/注册/刷新/登出/me）。
5. 方法级权限：`@PreAuthorize("hasRole('ADMIN')")` 标注管理端接口。
6. 统一异常：认证失败/无权限 → 401/403 响应（沿用 Result）。
7. 前端：`Login.vue`、Pinia `userStore`、axios 拦截器（注入/刷新 token）、路由守卫与角色菜单。
8. 测试：`JwtServiceTest`、登录/鉴权集成测试（MockMvc 或 Testcontainers）。

**验收**
- 未带 token 访问受保护接口 → 401；学生访问管理接口 → 403；管理员正常。
- 登录返回 token，前端可持久化并刷新；退出后 token 失效。
- 单测/集成测试通过；冒烟脚本更新（带 token）。

**风险**：前后端契约变化大；`UserController.login` 空壳被替换需同步前端登录流程。

---

### P3 — 数据层重建 + SQL IO 优化

**目标**：重设计 schema（一致性 + 约束 + 索引），用 Flyway 管理，迁移现有数据，消除 N+1 与慢查询。

**范围**
- 统一命名：时间列统一 `created_at`/`updated_at`；逻辑删除 `is_deleted` 全表统一（补齐 video 域）。
- 补全主外键与索引（见 §2.2 缺口）。
- 数据一致性：`status` 等用统一枚举（如考试状态 `ONGOING/FINISHED/GRADED`）。
- 金额用 `decimal` 一致；`question.score` int 与 `paper.total_score` decimal 统一策略。
- Flyway：`V1__baseline.sql`（= 当前基线）、`V2__redesign.sql`（结构演进）、`V3__seed.sql`（种子）。
- 数据迁移：用 `docs/db/baseline/*.sql` + 迁移脚本，保留现有题库/视频等内容。

**索引建议（最小集）**
- `questions(category_id)`、`questions(type)`、`questions(difficulty)`
- `question_choices(question_id)`、`question_answers(question_id)`
- `categories(parent_id)`
- `paper(status)`
- `paper_question(paper_id)`、`paper_question(question_id)`
- `exam_records(exam_id)`、`exam_records(status)`、`exam_records(student_name)`
- `answer_record(exam_record_id)`、`answer_record(question_id)`

**SQL IO 优化任务**
1. 审计全部 Mapper（含 `resources/mapper/*.xml`）与 LambdaQuery，消除循环内查询（N+1）。
2. 分页统一走 `IPage` + 索引；避免 `like '%x%'` 无索引全表。
3. 批量写：`saveBatch`/`updateBatchById` 已用，检查分批大小与事务边界。
4. 连接池参数已优化（P1）；补慢查询日志与 EXPLAIN 抽查。
5. 判卷路径的批量 `updateBatchById` 与汇总查询优化。

**任务分解**
1. 定稿目标 schema（ER 图 + DDL），评审。
2. 接入 Flyway（依赖、配置、`V1` 基线 = 当前 dump）。
3. 编写 `V2` 结构迁移 + 数据回填脚本，在副本库演练。
4. 逐表加索引/约束/统一列名；回归冒烟。
5. SQL IO 审计与重构 + 基准对比（前后 EXPLAIN/耗时）。
6. 更新实体/Mapper 映射，跑全量冒烟与单测。

**验收**
- `flyway migrate` 在空库可重建；在旧库可平滑升级且数据不丢。
- 关键查询 EXPLAIN 走索引；核心接口 P95 耗时较基线下降（记录数据）。
- 冒烟全绿。

**风险**：迁移不可逆 → 必须先在副本库演练并备份；Flyway 基线需与现有库精确对齐（注意 dump 里的 GTID 行，见 §9.8）。

---

### P4 — 深度分层 + 核心域重构

**目标**：feature 分包 + DTO/VO 分离 + MapStruct + 去循环依赖 + AI 判卷异步化。

**范围**
- 按 §3.1 的域重新分包（移动/拆分现有 controller/service/entity）。
- 入参用 `dto`（带校验），出参用 `vo`；实体不直接出入接口。
- 引入 MapStruct（`@Mapper` + `Mappers.getMapper` 或 Spring 组件扫描）。
- 消除 `spring.main.allow-circular-references`：拆分 `ExamRecordService`/`KimiService` 等互相依赖（用接口/事件/单向依赖）。
- AI 判卷异步：提交后置状态 `GRADING`，`@Async` 后台判卷，前端轮询成绩或 WebSocket/SSE 推送。
- 收敛 `Result.success(String)` 重载陷阱（重命名或标注）。
- 处理 `StartExamVo.studentName` 默认值（`"zhangsan"`）导致 `@NotBlank` 近乎失效的问题。

**任务分解**
1. 制定包结构与迁移映射表；建 `dto/vo/convert` 骨架。
2. 逐域迁移（user → category/question → paper → exam → video → banner/notice/stats）。
3. 引入 MapStruct 替换手写转换。
4. 去循环依赖，移除该 yml 开关。
5. 判卷异步化 + 状态机 + 前端轮询。
6. 补单测/集成测试；冒烟回归。

**验收**：`allow-circular-references` 关闭后启动正常；接口契约不变（路径/字段）；判卷接口立即返回、结果可轮询到；测试全绿。

**风险**：大范围移动文件易漏改；需强回归（冒烟 + 前端联调）。

---

### P5 — 补齐缺失业务

**目标**：实现前端已存在但后端缺失的域，并补齐现有空实现桩。

**范围与数据模型（新增表，P3 建或此阶段追加迁移）**
- 企业面试真题：`companies`、`interview_questions`（公司/技术方向/难度/题目/答案）、`interview_question_stats`（浏览/热度）。
- 模拟面试：`mock_interviews`、`mock_interview_answers`（对话式问答 + AI 评分）。
- 邀请码：`invite_codes`（码、状态、归属、过期）、`invite_code_usages`。
- 积分：`user_credits`（余额）、`credit_transactions`（流水）。
- 用户投稿/待审：复用 `questions` + 审核字段，或独立 `pending_questions`。
- 企业管理：`companies` 的公司 CRUD。
- **补齐空实现桩**：`NoticeServiceImpl`(7)、`PaperServiceImpl`(1)、`StatsServiceImpl`(1)、`VideoCategoryServiceImpl`(1)、`VideoServiceImpl`(3) —— 见 §9.6。

**接口（对齐前端 `src/api/*.js`）**
- `/api/interview-questions/{list,{id},hot,latest,{id}/view,stats/direction,stats/company}`
- `/api/mock-interview/{start,submit-answer,{id}/complete,user/{uid}/records}`
- `/interview/codes/{,generate,activate,{id},invitees,request}`、`/interview/{result,history,statistics,share,credits/history}`
- `/api/user-interview-credits/{user/{uid},active/{uid}}`
- `/api/user-contributions/upload`、`PendingQuestionManage` 审核接口、`CompanyManage` 接口

**任务分解**
1. 逐域：DDL 迁移 → entity/mapper → service → controller → 单测。
2. 邀请码：生成/激活/核销 + 积分发放事务。
3. 模拟面试：AI 对话（复用 Kimi 客户端）+ 评分。
4. 投稿/待审：上传 + 审核流转。
5. 补齐既有空实现桩并加测试。
6. 冒烟脚本扩展覆盖新域。

**验收**：前端对应页面全部可用；新接口有单测/集成测试；冒烟扩展后全绿。

**风险**：工作量最大；需与 P4 分层范式一致；AI 调用成本与限流。

---

### P6 — 前端工程化 + TypeScript

**目标**：Vite 7 + TS 迁移 + 统一请求层 + env + lint。

**范围**
- 升级：Vite 4→7、`vue`/`vue-router`/`pinia`/`element-plus`/`echarts`/`axios` 最新稳定版。
- TS 迁移：`.js`→`.ts`/`.vue`（`<script setup lang="ts">`），配置 `tsconfig.json`、`env.d.ts`。
- 统一 `request.ts`：baseURL 取 `import.meta.env.VITE_API_BASE_URL`（默认 `http://localhost:8090`）；请求注入 JWT；响应按 `Result.code` 处理；401 跳登录。
- 环境变量：`.env.development`/`.env.production`。
- 状态：Pinia `userStore`/`examStore` 等。
- 目录规范：`api/`、`stores/`、`types/`、`components/`、`views/`、`router/`。
- 代码质量：ESLint + Prettier；删除 `console.log`。
- 路由：登录守卫、角色菜单、懒加载，修正与后端一致的路径。

**任务分解**
1. 升级依赖 + Vite 7 配置 + 别名 `@`。
2. 接入 TS + ESLint/Prettier（先允许 `allowJs` 渐进）。
3. 重写 `request` + env + token 拦截。
4. 逐模块迁 TS（api → stores → components → views）。
5. 路由/菜单/守卫重构。
6. 构建验证 + 联调后端 8090。

**验收**：`npm run build` 通过；类型检查通过；全站可登录并调用 8090；无 console 残留。

**风险**：42 个视图迁移量大；需与 P2/P5 接口契约同步。

---

### P7 — 前端 UI 重做

**目标**：设计系统 + 统一布局 + 核心页面重写，达到作品集级观感。

**范围**
- 设计令牌：色板/字体/间距/圆角/阴影（可基于 Element Plus 主题变量定制）。
- 布局：前台（首页/视频/真题/刷题/考试）+ 管理台（侧栏/顶栏/面包屑）两套骨架。
- 核心页面重写：首页、考试流程（选择→作答→结果→排行）、视频列表/详情、题库管理、组卷、成绩管理。
- 组件库：统一表格/表单/空状态/加载态/确认弹窗。
- 响应式适配。

**任务分解**：设计令牌 → 布局骨架 → 公共组件 → 逐页重写 → 走查。

**验收**：视觉一致、交互可用、无回归；关键流程（考试/判卷/成绩）全通。

**风险**：工作量最大且主观；建议先在少量代表页确认风格再全量。

---

### P8 — 测试 / 文档 / 交付

**目标**：完整测试与文档，可一键起的开发环境。

**范围**
- 单测/集成测试：service 层单测 + MockMvc/Testcontainers 集成测试（MySQL/Redis/MinIO）。
- `README.md`：架构、启动、环境变量、端口、DB 迁移、常见问题。
- OpenAPI：导出 `openapi.json` 作为前后端契约。
- `docker-compose.yml`：Redis + MinIO（MySQL 本机）；可选后端镜像 + 前端 nginx。
- （可选）CI：GitHub Actions 跑 `mvn verify` + 前端 build。

**任务分解**：测试框架与样例 → 覆盖率提升 → README/契约 → compose/CI。

**验收**：`docker compose up` 起中间件；`mvn verify` 与前端 build 在 CI 绿；README 可让新人 30 分钟跑起来。

---

## 8. 工程约定（P1 已建立，后续沿用）

### 8.1 构建与运行

```bash
# 本机未装 mvn，用 IDEA 内置 Maven；固定 JDK 21
export JAVA_HOME=/Users/qyk9527/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home
export PATH="$JAVA_HOME/bin:/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin:$PATH"

cd /Users/qyk9527/ideaProject/exam_system_online
mvn -q clean package -DskipTests                 # 打包
java -jar target/exam_system_online-1.0-SNAPSHOT.jar   # 启动（端口 8090，须从仓库根目录启动）
mvn test -Dtest=ResultTest,GlobalExceptionHandlerTest  # 指定单测
./scripts/smoke.sh                                # 冒烟（9 OK + SMOKE PASS）
```

### 8.2 本地配置
```bash
cp application-local.yml.example application-local.yml   # 填 KIMI_API_KEY；可选开 SQL 日志
```
- 密钥只放 `application-local.yml`（已 gitignore）或环境变量；**禁止提交到源码/配置**（`git grep -n 'sk-' -- 'src/**' 'application.yml' 'pom.xml'` 必须为空）。

### 8.3 流程与分支
- 每个阶段：**设计（spec）→ 计划（plan）→ 实施 → 评审**。
- 用特性分支（如 `refactor/p2-auth`），完成后合并 `main`；重要节点打标签。
- 文档落位：设计 `docs/superpowers/specs/`、计划 `docs/superpowers/plans/`、完成报告 `docs/superpowers/`。
- 提交信息语义化（build/perf/security/fix/feat/docs/chore/refactor/test）。

### 8.4 回归底线
- 改动后必须：`mvn test` 全绿 + `./scripts/smoke.sh` 全绿（扩展新域后同步更新脚本）。

---

## 9. 遗留缺陷与风险登记

来自 P1 完成报告，接手时需知悉：

1. **【必须人工】密钥轮换**：旧 Moonshot key `sk-sX3N2...` 仍在 git 历史（初始提交 `5dfd399`）。工作区删除≠消除泄露；须在 Moonshot 后台吊销/轮换，必要时 `git filter-repo` 清理历史。
2. **基线明文凭据**：`docs/db/baseline/online_exam_data_baseline.sql` 固化 `admin/admin123`；`application.yml` 本地 `root/root123456`、`minioadmin/minioadmin`。仅限本地；若公开需一并处理。
3. **循环依赖开关**：`spring.main.allow-circular-references: true` 仍开，P4 消除后移除。
4. **`Result.success(String)` 重载陷阱**：`success("x")` 会命中 `success(String message)`，导致 `data=null`。想返回字符串数据需 `Result.success((Object) "x")`；P4 用命名区分收敛（66 处调用依赖，勿直接删）。
5. **空实现 Service 桩（`return null`）**：`NoticeServiceImpl`(7 方法全空)、`PaperServiceImpl`(1)、`StatsServiceImpl`(1)、`VideoCategoryServiceImpl`(1)、`VideoServiceImpl`(3)。对应接口返回 HTTP 200 + 空体；P5 补齐（`/api/notices/*` 尤重，前端已用）。

   **5b.（2026-10-01 实测新增）controller 层还有 7 个未登记的 `Result.success(null)` 桩**（service 层根本没实现对应方法，属"要写业务"而非"忘了接线"）：
   - `QuestionController`：`createQuestion`、`updateQuestion`（→ 前端新建/编辑题目后拿不到返回体）、`getQuestionsByCategory`、`getQuestionsByDifficulty`、`getRandomQuestions`（→ **刷题页与按分类/难度筛选全空**）
   - `ExamController.getMyRecords`（→ 「我的考试记录」空）
   - `UserController.login`（已在 P2 计划内）
   另有 9 处 `Result<Void>` 返回 `success(null)` 是合法的（增删改无返回体），不算桩。实测公告 3 个接口是 **HTTP 200 + 0 字节空 body**（连 JSON 都没有）。

   **5c.（2026-10-01 实测新增）`scripts/smoke.sh` 断言过松**：只校验 HTTP 与 `code==200`，不校验 `data` 非空，导致 `/api/stats/overview`、`/api/videos` 这类返回 `data:null` 的桩接口被判 **OK**（本次 9 项全 PASS 但其中至少 2 项实际取不到数据）。P5 给冒烟加一条「关键只读接口必须断言 data 非空/非 null」。
6. **HTTP 约定**：成功/业务/校验/方法/媒体类型错误均 HTTP 200 + 响应体 `code`；仅未映射路径返回真正 404。前端按 `code` 判断，勿只依赖 HTTP 状态。
7. **CORS**：默认仅放行 `http://localhost:3001`；其他来源需配 `app.cors.allowed-origins`。前端端口改为 8090 后，P6 需同步。
8. **DB 基线含 `SET @@GLOBAL.GTID_PURGED=...`**：导入非空实例会报错；P3 恢复脚本需去掉/调整该行。
9. **`MyBatisGeneratoir.java`** 是代码生成器工具（非测试），位于 `src/test`，建议 P4 移出或 `@Disabled`。
10. **MinIO 状态**：Docker 里，当前未启动；涉及文件上传的接口联调前需先起 Redis/MinIO。

来自 P2（2026-10-01 实测，详见 `docs/superpowers/2026-10-01-p2-auth-completion.md` §4）：

11. **`/api/exams/{id}` 无数据级 ownership 校验**：学生令牌可读到别人的考试记录（实测 `GET /api/exams/38` → 200）。URL 规则只管角色，归属要在 service 层按 token 的 userId 过滤 → 归 P4。
12. **删除不存在的资源返回 `code:500`「系统繁忙」而非 404/业务码**（实测 `DELETE /api/categories/999999`），被 `GlobalExceptionHandler` 的兜底 `Exception` 吞掉 → 归 P5。
13. **匿名访问 `/home` 会被弹到登录页**：计划的前台放行清单不含题库/排行/统计，故首页热门题目 401。需要公开演示的话，应在 P5 提供脱敏只读接口（现有 `questionApi.popular` 响应含答案与 `isCorrect`，不能直接放行）。

---

## 10. P1 实施范式（供后续阶段复制）

P1 采用「子代理逐任务 + 任务级评审 + 最终整分支评审」的执行方式，效果良好，建议后续沿用：

1. 写 spec（`docs/superpowers/specs/`）→ 用户评审 → 写 plan（`docs/superpowers/plans/`，任务级、含真实命令/代码/验收）。
2. 在特性分支上，**逐任务**派实现者子代理；每个任务后派评审子代理（对照 brief + 全局约束 + diff）。
3. 发现与 plan 冲突/新缺陷 → 记录并修复；阶段末尾做**整分支评审**。
4. 全绿后合并 `main`，删除分支。
- 关键：任务粒度小、每步可验证；评审独立于实现；遗留项显式登记。

---

## 11. 附录

### 11.1 后端接口现状（14 控制器）

```
/api/auth:        POST /login /refresh /logout  GET /me   (P2 新增，JWT + Redis refresh)
/api/user:        POST /login(委托 AuthService)  GET /check-admin/{userId}(真实判角色)
/api/questions:   GET /list  GET /{id}  POST /  PUT /{id}  DELETE /{id}
                  GET /category/{id}  GET /difficulty/{d}  GET /random  GET /popular  POST /popular/refresh
/api/questions/batch: GET /template  POST /preview-excel /import-excel /ai-generate /import-questions /validate
/api/papers:      GET /list  POST /  PUT /{id}  POST /ai  GET /{id}  POST /{id}/status  DELETE /{id}
/api/exams:       POST /start  POST /{id}/submit  POST /{id}/grade  GET /{id}  GET /records
/api/exam-records:GET /list  GET /{id}  DELETE /{id}  GET /ranking
/api/categories:  GET /  GET /tree  POST /  PUT /  DELETE /{id}
/api/notices:     GET /active(空)  /latest(空)  /list(空)  /{id}  POST /add(空)  PUT /update(空)  DELETE /delete/{id}(空)  PUT /toggle/{id}(空)
/api/banners:     POST /upload-image  GET /active /list /{id}  POST /add  PUT /update  DELETE /delete/{id}  PUT /toggle/{id}
/api/videos:      GET /  /{id} /popular /latest  POST /{id}/view  POST /{id}/like  POST /submit
/api/admin/videos:GET /  POST /upload  POST /{id}/audit /offline  DELETE /{id}  GET /statistics  GET /{id}/stats
/api/video-categories: GET / /tree /top /children/{pid} /{id}  POST /  PUT /  DELETE /{id}
/api/stats:       GET /overview  GET /test
/files/**         GET /**  (静态/存储代理)
```

### 11.2 前端页面清单（43 个视图）

```
首页/通用: Home, Welcome, AdminLayout, ActivateCode
考试:     PaperListForExam, ExamStart, Exam, ExamResult, ExamRanking, ScoreManage
刷题:     Practice
题库:     QuestionManage, PendingQuestionManage, CategoryManage, PaperManage, PaperCreate
视频:     VideoList, VideoDetail, VideoManage, VideoCategoryManage
运营:     BannerManage, NoticeManage
面试:     InterviewQuestionList, InterviewQuestionDetail, InterviewQuestionManage,
          InterviewPractice, InterviewResult, MockInterview, MockInterviewDetail,
          InterviewCodes, CompanyManage, CompanyDetail, CompanyQuestionCategoryManage
```
（`api/`：exam、paper、video、videoCategory、interviewQuestion；`utils/request.js`；`router/index.js`。）

### 11.3 前端 → 后端缺失接口对照
见 §2.4。P5 需实现：`/api/interview-questions/**`、`/api/mock-interview/**`、`/interview/**`、
`/api/user-interview-credits/**`、`/api/user-contributions/**`、Company 相关。

---

## 12. 附：文档索引

| 文档 | 作用 |
|---|---|
| `docs/superpowers/MASTER-DEVELOPMENT-PLAN.md` | **本文档**：总计划 + 路线 + 决策 + 遗留 |
| `docs/superpowers/specs/2026-09-30-p1-backend-foundation-design.md` | P1 设计（含总体路线原始定义） |
| `docs/superpowers/plans/2026-09-30-p1-backend-foundation.md` | P1 实施计划 |
| `docs/superpowers/2026-09-30-p1-completion.md` | P1 完成报告 + 遗留事项 |
| `docs/db/baseline/*.sql` | P0 数据库基线与数据 |
| `scripts/smoke.sh` | 端到端冒烟 |
| 前端 `docs/superpowers/plans/2026-10-01-p6-frontend-ts-migration.md` | **P6 前端进度与交接**（已完成清单 / 34 个待迁视图 / 迁移期兼容层删除清单 / 契约实测结论） |
