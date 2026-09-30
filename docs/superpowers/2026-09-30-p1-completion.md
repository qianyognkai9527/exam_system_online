# P1 后端底座现代化 · 完成报告

- 日期：2026-09-30
- 分支：`refactor/p1-foundation`
- 回退基线标签：`pre-refactor-baseline`
- 应用端口：`8090`（原 `8080` 被无关进程 `qcsz/4T-open-user-bff` 占用，本阶段固定为 8090）

## 1. 目标版本对照表

| 组件 | 升级前 | 当前（目标） | 备注 |
|---|---|---|---|
| Spring Boot | 3.0.5 | **3.5.3** | parent 版本 |
| Java | 17 | **21** | `java.version` + `maven.compiler.release` |
| MyBatis-Plus | 3.5.3.2 | **3.5.9** | `mybatis-plus-spring-boot3-starter`；分页依赖拆出为 `mybatis-plus-jsqlparser` |
| FastJSON | fastjson 1.x `2.0.25` | **fastjson2 2.0.65** | 注解迁移（`@JSONField` 已移除） |
| springdoc-openapi | 无显式 | **2.8.6** | `springdoc-openapi-starter-webmvc-ui` |
| Knife4j | 4.4.0 | **4.5.0** | `knife4j-openapi3-jakarta-spring-boot-starter` |
| Redisson | 3.24.3 | **3.44.0** | |
| MinIO | 8.5.7 | **8.5.17** | |
| Apache POI | 5.2.4 | **5.4.1** | poi + poi-ooxml |
| Hutool | 5.8.3 | **5.8.36** | |
| FreeMarker | 2.3.32（显式 pin） | **2.3.34** | 去掉显式版本，改由 Boot 3.5.3 父 POM 管理（2.3.32 在 Boot 3.5.3 下缺少 `freemarker.ext.jakarta.jsp.TaglibFactory` 导致启动失败） |
| MySQL 驱动 | `${mysql.version}` | 父 POM 托管 | 移除显式版本 |
| Jackson | 显式 `jackson-databind` | 传递依赖 | 删除显式依赖块 |

## 2. P0 基线保护

- 分支：`refactor/p1-foundation`
- 标签：`pre-refactor-baseline`
- 基线文件：`docs/db/baseline/online_exam_schema_baseline.sql`、`docs/db/baseline/online_exam_data_baseline.sql`

## 3. 冒烟脚本

新增 `scripts/smoke.sh`（`APP_PORT=8090`）：`mvn clean package` → 后台启动 jar → 轮询
`/api/stats/overview` 就绪 → 逐个断言 10 个接口响应体含 `"code":200` → 退出码即结果。

### 3.1 实跑输出（真实结果）

```
等待应用启动（最多 120s）...
OK   /api/questions/list
OK   /api/papers/list
OK   /api/categories
OK   /api/categories/tree
OK   /api/videos
OK   /api/videos/popular
OK   /api/banners/active
FAIL /api/notices/active ->
OK   /api/stats/overview
OK   /api/video-categories/tree
SMOKE FAIL（应用日志：/tmp/exam_smoke_app.log）
```

- 退出码：`1`（9 个 `OK`，1 个 `FAIL`，未达 `SMOKE PASS`）。
- **失败根因（预先存在，非 P1 引入）**：`NoticeServiceImpl` 的 7 个方法在基线
  `pre-refactor-baseline` 中即为 `return null` 的未实现桩（`git log` 仅 `Initial commit`），
  控制器返回 `null` → HTTP 200 且响应体为空 → 断言 `"code":200` 失败。该缺陷不属于 P1
  （版本/依赖/密钥/连接池/CORS/日志/异常校验）范围，未在本任务中修改业务实现。

## 4. 阶段验收清单（真实输出）

```
1) 明文密钥:
  CLEAN
2) CrossOrigin 残留:
  CLEAN
3) Log4j2 残留:
  CLEAN
4) 分支/标签:
refactor/p1-foundation
pre-refactor-baseline
5) 基线文件:
total 224
drwxr-xr-x  4 qyk9527  staff    128  9月 30 15:42 .
drwxr-xr-x  3 qyk9527  staff     96  9月 30 15:37 ..
-rw-r--r--  1 qyk9527  staff  92118  9月 30 15:42 online_exam_data_baseline.sql
-rw-r--r--  1 qyk9527  staff  19741  9月 30 15:42 online_exam_schema_baseline.sql
```

- 验收命令（逐条）：
  - `git grep -n 'sk-' -- 'src/**' 'application.yml' 'pom.xml'` → **CLEAN**（无明文密钥）
  - `grep -rc '@CrossOrigin' src/main/java | grep -v ':0'` → **CLEAN**
  - `grep -rn '@Log4j2' src/main/java` → **CLEAN**（已统一为 `@Slf4j`）
  - `git branch --show-current` → `refactor/p1-foundation`；`git tag | grep pre-refactor-baseline` → 存在
  - `ls -la docs/db/baseline` → schema/data 两份 SQL 齐全

## 5. Knife4j / springdoc 兼容性实测

| 请求 | 结果 |
|---|---|
| `GET /doc.html` | HTTP **200**（Knife4j UI 正常返回，1903 bytes） |
| `GET /v3/api-docs` | HTTP **200**（OpenAPI `3.1.0`，72564 bytes） |
| `GET /swagger-ui/index.html` | HTTP **200**（734 bytes） |

结论：Knife4j 4.5.0 + springdoc 2.8.6 在 Spring Boot 3.5.3 / Java 21 下**兼容**，
OpenAPI JSON 正常生成、UI 可访问。

## 6. 遗留事项

1. **`spring.main.allow-circular-references: true` 仍开启**：本阶段保留以确保平滑迁移，
   计划在 **P4** 消除循环依赖后移除。
2. **`src/test/java/MyBatisGeneratoir.java` 是开发工具（MyBatis-Plus 代码生成器），
   非真实测试**：硬编码 `outputDir`/tables，裸 `mvn test` 时可能被当作测试执行；
   后续应移出 `src/test` 或重命名/加 `@Disabled`。
3. **端口由 8080 改为 8090**：因 8080 被无关进程 `qcsz/4T-open-user-bff` 占用；
   冒烟脚本、文档与本地验证统一使用 8090，需同步前端/部署配置。
4. **`Result.success(String)` 重载陷阱（没坑）**：`Result.success("x")` 会绑定到
   `success(String message)`（消息重载）而非 `success(T data)`，导致数据被当作消息、
   `data=null`。66 处既有调用依赖此重载，暂不能删除；调用方若想返回字符串数据需显式
   转型（如 `Result.success((Object) "x")`）。P4 可用静态方法命名区分收敛。
5. **兜底异常处理器对未映射的 MVC 异常仍返回 HTTP 200**：仅
   `NoResourceFoundException` 映射为真正的 HTTP 404；`HttpRequestMethodNotSupportedException`（应 405）、
   `HttpMediaTypeNotSupportedException`（应 415）、`MissingServletRequestParameterException`（应 400）
   等仍落到 `handleException(Exception)`，返回 HTTP 200 + `{"code":500,...}`。浏览器/客户端
   若依赖 HTTP 状态码会误判；扩面映射列为 P4 后续候选项。
6. **`scripts/smoke.sh` 相对任务书有一处最小修正**：任务书原文
   `echo "SMOKE FAIL（应用日志：$LOG）"` 在 macOS 自带 bash 3.2 + `set -u` 下，全角右括号会被
   并入变量名解析，触发 `LOG…: unbound variable` 并中断脚本；改为 `${LOG}` 以稳定输出诊断行。
   接口清单、`APP_PORT`、其余逻辑与任务书完全一致。

## 7. 结论

- P1 目标版本、密钥外置、Hikari/日志/Actuator、CORS 白名单、`@Slf4j` 统一、
  统一响应/异常/校验均已完成并通过验收清单（1/2/3 CLEAN、分支/标签/基线齐全）。
- 冒烟脚本已交付并可重复运行，**当前因预先存在的 `NoticeServiceImpl` 未实现桩导致 1 个接口
  断言失败**（HTTP 200 空响应体），故本阶段状态为 `DONE_WITH_CONCERNS`。
