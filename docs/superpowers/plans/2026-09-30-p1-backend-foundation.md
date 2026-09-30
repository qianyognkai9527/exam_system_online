# P0+P1 后端底座现代化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为在线考试系统后端建立可回退基线，并把技术底座从 Spring Boot 3.0.5/Java 17 升级到 Spring Boot 3.5.3/Java 21，完成配置密钥外置、连接池/CORS/日志/Actuator 加固、统一异常与校验。

**Architecture:** 保持单模块、单 Spring Boot 入口不变；本阶段只改 POM、配置、少量新增 `config`/`common` 类与既有类的注解，不改包结构、不改接口契约、不动前端。

**Tech Stack:** Java 21、Spring Boot 3.5.3、MyBatis-Plus 3.5.9（`mybatis-plus-spring-boot3-starter` + `mybatis-plus-jsqlparser`）、fastjson2 2.0.65、Knife4j 4.5.0 + springdoc 2.8.6、Redisson 3.44.0、MinIO 8.5.17、POI 5.4.1、Hutool 5.8.36、JUnit 5。

## Global Constraints

- 工作分支：`refactor/p1-foundation`（已创建）；回退点标签：`pre-refactor-baseline`（已存在）。本计划所有提交都在这条分支。
- 后端端口固定 **8090**，不得更改。（2026-09-30 决策变更：原定 8080 被本机另一项目 `qcsz/4T-open-user-bff` 占用，故本改为 8090；前端 `request.js` 的 `http://localhost:8080` 将在 P6 改为环境变量。）
- **禁止提交任何密钥**；`application-local.yml` 必须被 gitignore，提交前 `git grep -n 'sk-'` 必须为空。
- `Result` 响应结构保持 `{code,message,data}` 不变（前端不改）。
- 不修改任何 URL 路径、请求/响应字段名。
- 构建环境（每条构建命令前先导出）：

```bash
export JAVA_HOME=/Users/qyk9527/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home
export PATH="$JAVA_HOME/bin:/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin:$PATH"
```

- 打包命令：`mvn -q clean package -DskipTests`；运行：`java -jar target/exam_system_online-1.0-SNAPSHOT.jar`。
- 单测命令：`mvn test -Dtest=<ClassName>`（**不要**裸跑 `mvn test`，避免无关测试拉起上下文）。
- 目标依赖版本（本地 `~/.m2` 均已存在，可离线构建）：Boot parent `3.5.3`、MP `3.5.9`、fastjson2 `2.0.65`、knife4j `4.5.0`、springdoc `2.8.6`、redisson `3.44.0`、minio `8.5.17`、poi `5.4.1`、hutool `5.8.36`。

---

## 文件结构（本阶段将创建/修改）

| 文件 | 责任 |
|---|---|
| `docs/db/baseline/online_exam_schema_baseline.sql` | 新建：当前库结构基线（P3 输入） |
| `docs/db/baseline/online_exam_data_baseline.sql` | 新建：当前库数据基线 |
| `.gitignore` | 修改：忽略本地配置 |
| `pom.xml` | 修改：版本与依赖治理 |
| `src/main/java/com/joker/ai/exam/entity/Banner.java` | 修改：去除 fastjson1 注解导入 |
| `src/test/java/MyBatisGeneratoir.java` | 修改：适配 MP 3.5.9 生成器 API |
| `src/main/resources/application.yml` | 修改：密钥外置、Hikari、CORS、actuator、mybatis 日志 |
| `application-local.yml.example` | 新建：本地配置模板（模板入库，真实文件忽略） |
| `src/main/java/com/joker/ai/exam/config/WebMvcConfig.java` | 新建：CORS 白名单 |
| `src/main/java/com/joker/ai/exam/common/ErrorCode.java` | 新建：错误码枚举 |
| `src/main/java/com/joker/ai/exam/common/BizException.java` | 新建：业务异常 |
| `src/main/java/com/joker/ai/exam/config/GlobalExceptionHandler.java` | 修改：统一异常处理 |
| `src/main/java/com/joker/ai/exam/controller/*.java`（11 个） | 修改：删除 `@CrossOrigin` |
| `src/main/java/com/joker/ai/exam/{controller,service/impl}/*.java`（6 个） | 修改：`@Log4j2` → `@Slf4j` |
| `src/test/java/com/joker/ai/exam/common/ResultTest.java` | 新建：Result 单测 |
| `src/test/java/com/joker/ai/exam/config/GlobalExceptionHandlerTest.java` | 新建：异常处理器单测 |
| `scripts/smoke.sh` | 新建：冒烟脚本 |

---

### Task 1: P0 冻结数据库基线并忽略本地配置

**Files:**
- Create: `docs/db/baseline/online_exam_schema_baseline.sql`
- Create: `docs/db/baseline/online_exam_data_baseline.sql`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: 本机 MySQL `localhost:3306`，库 `online_exam`（root/root123456）。
- Produces: 结构化 SQL 基线文件，供 P3 数据迁移使用。

- [ ] **Step 1: 导出结构基线**

```bash
cd /Users/qyk9527/ideaProject/exam_system_online
/usr/local/mysql/bin/mysqldump -uroot -proot123456 --no-data --routines --triggers online_exam \
  > docs/db/baseline/online_exam_schema_baseline.sql 2>/dev/null
wc -l docs/db/baseline/online_exam_schema_baseline.sql
```

Expected: 输出行数 > 100（15 张表）。

- [ ] **Step 2: 导出数据基线**

```bash
/usr/local/mysql/bin/mysqldump -uroot -proot123456 --no-create-info --complete-insert online_exam \
  > docs/db/baseline/online_exam_data_baseline.sql 2>/dev/null
wc -l docs/db/baseline/online_exam_data_baseline.sql
```

Expected: 行数 > 50。

- [ ] **Step 3: 忽略本地配置文件**

把下面两行追加到 `.gitignore` 末尾：

```gitignore
### Local config (secrets) ###
application-local.yml
application-local*.yml
```

- [ ] **Step 4: 验证忽略生效且基线可入库**

```bash
printf 'kimi:\n  api:\n    api-key: test\n' > application-local.yml
git check-ignore -v application-local.yml
rm application-local.yml
git status --short docs/db/baseline
```

Expected: `check-ignore` 打印匹配 `.gitignore` 的行；`git status` 显示两个新 SQL 文件为未跟踪。

- [ ] **Step 5: 提交**

```bash
git add .gitignore docs/db/baseline
git commit -m "chore(p0): 冻结 online_exam 结构与数据基线，忽略 application-local.yml"
```

---

### Task 2: 依赖与版本升级到 Boot 3.5.3 / Java 21 / MP 3.5.9

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/java/com/joker/ai/exam/entity/Banner.java`
- Modify: `src/test/java/MyBatisGeneratoir.java`

**Interfaces:**
- Consumes: 本地 `~/.m2` 中的目标版本。
- Produces: 可构建的工程；后续任务在此基础上加配置与代码。

- [ ] **Step 1: 升级 parent 与 properties**

把 `pom.xml` 的 `<parent>` 版本改为 `3.5.3`：

```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.3</version>
    </parent>
```

把整个 `<properties>` 块替换为：

```xml
    <properties>
        <java.version>21</java.version>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <knife4j.version>4.5.0</knife4j.version>
        <springdoc.version>2.8.6</springdoc.version>
        <redisson.version>3.44.0</redisson.version>
        <minio.version>8.5.17</minio.version>
        <poi.version>5.4.1</poi.version>
        <hutool.version>5.8.36</hutool.version>
        <fastjson2.version>2.0.65</fastjson2.version>
    </properties>
```

- [ ] **Step 2: 替换 MyBatis-Plus 依赖（Boot3 专用坐标 + jsqlparser）**

把原 `mybatis-plus-boot-starter` 依赖块替换为：

```xml
        <!-- MyBatis Plus (Spring Boot 3) -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- 分页插件依赖（3.5.9 起从主包拆出，缺失将导致 PaginationInnerInterceptor 编译失败） -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
```

把 `mybatis-plus-generator` 的版本 `3.5.3.2` 改为 `${mybatis-plus.version}`。

- [ ] **Step 3: 替换 fastjson 依赖（直连 fastjson2）**

把原 `com.alibaba:fastjson:2.0.25` 依赖块替换为：

```xml
        <!-- Fastjson2 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
```

- [ ] **Step 4: 版本参数化，去掉父 POM 已管理项的显式版本**

- hutool-all `<version>` → `${hutool.version}`
- poi / poi-ooxml `<version>` → `${poi.version}`
- knife4j `<version>` → `${knife4j.version}`
- redisson `<version>` → `${redisson.version}`
- minio `<version>` → `${minio.version}`
- 删除 `mysql-connector-j` 的 `<version>`（父 POM 管理）
- 删除 `jackson-databind` 依赖块（`spring-boot-starter-web` 已传递）
- 在 knife4j 依赖之后新增显式 springdoc 覆盖：

```xml
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
```

- [ ] **Step 5: 移除古代 lombok 插件**

删除整个 `lombok-maven-plugin`（`<groupId>org.projectlombok</groupId>` + `<artifactId>lombok-maven-plugin</artifactId>` + version `1.18.20.0` + 其 `<executions>`）的 plugin 块；保留 `spring-boot-maven-plugin`。

- [ ] **Step 6: 迁移 Banner 的 fastjson1 注解**

`src/main/java/com/joker/ai/exam/entity/Banner.java` 第 3 行：

```java
import com.alibaba.fastjson.annotation.JSONField;
```

**删除这一行**（当前 `@JSONField` 无实际使用；若确实在使用，则改为
`import com.alibaba.fastjson2.annotation.JSONField;`）。删除后确认文件内再无
`com.alibaba.fastjson.`（无 `2`）的导入。

- [ ] **Step 7: 适配代码生成器 API（MP 3.5.9）**

`src/test/java/MyBatisGeneratoir.java` 第 18 行 `.enableSwagger()` 在 3.5.9 已移除。
把第 17–20 行改为：

```java
                    builder.author("joker") // 设置作者
                            .outputDir("/User/qyk9527/ideaProject/exam_system_online/src/main/java"); // 指定输出目录
```

- [ ] **Step 8: 构建验证**

```bash
export JAVA_HOME=/Users/qyk9527/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home
export PATH="$JAVA_HOME/bin:/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin:$PATH"
cd /Users/qyk9527/ideaProject/exam_system_online
mvn -q clean package -DskipTests
```

Expected: BUILD SUCCESS，生成 `target/exam_system_online-1.0-SNAPSHOT.jar`。

- [ ] **Step 9: 启动验证**

```bash
java -jar target/exam_system_online-1.0-SNAPSHOT.jar > /tmp/p1_boot.log 2>&1 &
sleep 30
curl -s http://localhost:8090/api/stats/overview | head -c 200
kill %1
```

Expected: 返回包含 `"code":200` 的 JSON。

- [ ] **Step 10: 提交**

```bash
git add pom.xml src/main/java/com/joker/ai/exam/entity/Banner.java src/test/java/MyBatisGeneratoir.java
git commit -m "build: 升级 Spring Boot 3.5.3 / Java 21 / MyBatis-Plus 3.5.9 / fastjson2"
```

---

### Task 3: 配置与密钥外置

**Files:**
- Modify: `src/main/resources/application.yml`
- Create: `application-local.yml.example`（仓库根目录）

**Interfaces:**
- Consumes: Task 2 的构建产物。
- Produces: 端口 8090 + `KIMI_API_KEY` 占位 + `application-local.yml` 覆盖机制。

- [ ] **Step 0: 端口改为 8090**

把 `application.yml` 顶部的 `server.port: 8080` 改为 `8090`（8080 已被本机
`qcsz/4T-open-user-bff` 占用）。后续所有 curl/冒烟命令使用 8090。

- [ ] **Step 1: 改写 kimi 配置为占位符**

把 `application.yml` 中 `kimi.api.api-key` 一行（含明文 `sk-...`）替换为：

```yaml
    api-key: ${KIMI_API_KEY:}   # 真实值放 application-local.yml 或环境变量
```

- [ ] **Step 2: 增加本地配置导入**

在 `application.yml` 的 `spring:` 下新增（放在 `spring:` 第一行）：

```yaml
  config:
    import: optional:classpath:application-local.yml
```

- [ ] **Step 3: 新建本地配置模板**

创建仓库根目录 `application-local.yml.example`：

```yaml
# 复制为 application-local.yml（已被 gitignore）后填入真实值
kimi:
  api:
    api-key: sk-your-moonshot-key

# 本地调试时打开 MyBatis SQL 日志
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- [ ] **Step 4: 验证无明文密钥残留**

```bash
cd /Users/qyk9527/ideaProject/exam_system_online
git grep -n 'sk-' -- . ':!*.example' || echo "CLEAN: 无明文密钥"
```

Expected: 输出 `CLEAN: 无明文密钥`（或只匹配到 `.example` 以外为空）。

- [ ] **Step 5: 启动验证（无本地文件也能起）**

```bash
mvn -q clean package -DskipTests
java -jar target/exam_system_online-1.0-SNAPSHOT.jar > /tmp/p1_boot.log 2>&1 &
sleep 30
curl -s http://localhost:8090/api/stats/overview | head -c 120
kill %1
```

Expected: 返回 `"code":200`（api-key 为空不影响启动）。

- [ ] **Step 6: 提交**

```bash
git add src/main/resources/application.yml application-local.yml.example
git commit -m "chore: 密钥外置到 application-local.yml，新增本地配置模板"
```

---

### Task 4: 连接池 / Actuator / MyBatis 日志加固

**Files:**
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Produces: 修正后的 Hikari 参数、受控的 Actuator 端点、静默的 MyBatis 日志默认值。

- [ ] **Step 1: 替换 Hikari 配置**

把 `spring.datasource.hikari` 整块替换为：

```yaml
    hikari:
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      maximum-pool-size: 20
      minimum-idle: 5
      pool-name: EXAMHikariPool
```

- [ ] **Step 2: 收敛 MyBatis 日志**

把 `mybatis-plus.configuration.log-impl` 从 `org.apache.ibatis.logging.stdout.StdOutImpl`
改为：

```yaml
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

（本地需 SQL 日志时由 `application-local.yml` 覆盖，见 Task 3 模板。）

- [ ] **Step 3: 收敛 Actuator 端点**

在 `application.yml` 末尾追加：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- [ ] **Step 4: 验证**

```bash
cd /Users/qyk9527/ideaProject/exam_system_online
mvn -q clean package -DskipTests
java -jar target/exam_system_online-1.0-SNAPSHOT.jar > /tmp/p1_boot.log 2>&1 &
sleep 30
echo "--- health ---"; curl -s http://localhost:8090/actuator/health
echo; echo "--- metrics(应 404) ---"; curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/actuator/metrics
kill %1
```

Expected: health 返回 `{"status":"UP"...}`；metrics 返回 `404`。

- [ ] **Step 5: 提交**

```bash
git add src/main/resources/application.yml
git commit -m "perf: 修正 Hikari 连接池参数，收敛 actuator 端点与 MyBatis 日志"
```

---

### Task 5: CORS 收敛（白名单替换通配符）

**Files:**
- Create: `src/main/java/com/joker/ai/exam/config/WebMvcConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify（删除 `@CrossOrigin`）:
  `QuestionController.java`、`ExamController.java`、`QuestionBatchController.java`、
  `CategoryController.java`、`NoticeController.java`、`ExamRecordController.java`、
  `PaperController.java`、`FileController.java`、`BannerController.java`、
  `StatsController.java`、`UserController.java`

**Interfaces:**
- Produces: `WebMvcConfig` 读取 `app.cors.allowed-origins`；所有接口共享该策略。

- [ ] **Step 1: 新增 CORS 配置类**

创建 `src/main/java/com/joker/ai/exam/config/WebMvcConfig.java`：

```java
package com.joker.ai.exam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3001}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 2: 配置允许来源**

在 `application.yml` 的 `management:` 之前追加：

```yaml
app:
  cors:
    allowed-origins: http://localhost:3001
```

- [ ] **Step 3: 删除所有 `@CrossOrigin`**

```bash
cd /Users/qyk9527/ideaProject/exam_system_online
grep -rl '@CrossOrigin' src/main/java
```

对列出的每个文件，删除类上那一行 `@CrossOrigin(...)`（多为
`@CrossOrigin(origins = "*")` 或 `@CrossOrigin`）。完成后确认：

```bash
grep -rn '@CrossOrigin' src/main/java && echo "还有残留" || echo "OK: 已全部删除"
```

Expected: `OK: 已全部删除`。

- [ ] **Step 4: 构建并验证 CORS 预检**

```bash
mvn -q clean package -DskipTests
java -jar target/exam_system_online-1.0-SNAPSHOT.jar > /tmp/p1_boot.log 2>&1 &
sleep 30
echo "--- allowed origin ---"
curl -s -i -X OPTIONS http://localhost:8090/api/questions/list \
  -H "Origin: http://localhost:3001" -H "Access-Control-Request-Method: GET" | grep -i "access-control-allow-origin"
echo "--- disallowed origin(应无 allow-origin) ---"
curl -s -i -X OPTIONS http://localhost:8090/api/questions/list \
  -H "Origin: http://evil.example" -H "Access-Control-Request-Method: GET" | grep -i "access-control-allow-origin" || echo "OK: 未放行"
kill %1
```

Expected: 第一条输出 `Access-Control-Allow-Origin: http://localhost:3001`；
第二条输出 `OK: 未放行`。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/joker/ai/exam/config/WebMvcConfig.java src/main/resources/application.yml src/main/java/com/joker/ai/exam/controller
git commit -m "security: 用 CORS 白名单替换 11 处 @CrossOrigin(*) 通配"
```

---

### Task 6: 日志框架统一为 SLF4J

**Files:**
- Modify: `PaperController.java`、`PaperServiceImpl.java`、`ExamRecordServiceImpl.java`、
  `FileUploadServiceImpl.java`、`KimiServiceImpl.java`、`QuestionServiceImpl.java`

**Interfaces:**
- Produces: 全项目统一 `@Slf4j`，不再依赖 Boot 未引入的 log4j2 API。

- [ ] **Step 1: 逐文件替换注解与导入**

对列出的 6 个文件（均在 `src/main/java/com/joker/ai/exam/` 下）：

- 把 `import lombok.extern.log4j.Log4j2;` 改为 `import lombok.extern.slf4j.Slf4j;`
- 把类上的 `@Log4j2` 改为 `@Slf4j`

例如 `ExamRecordServiceImpl.java`：

```java
// 原
import lombok.extern.log4j.Log4j2;
...
@Log4j2
public class ExamRecordServiceImpl ...
// 改为
import lombok.extern.slf4j.Slf4j;
...
@Slf4j
public class ExamRecordServiceImpl ...
```

- [ ] **Step 2: 验证无残留**

```bash
cd /Users/qyk9527/ideaProject/exam_system_online
grep -rn "lombok.extern.log4j.Log4j2\|@Log4j2" src/main/java && echo "还有残留" || echo "OK: 已统一 Slf4j"
```

Expected: `OK: 已统一 Slf4j`。

- [ ] **Step 3: 构建验证**

```bash
mvn -q clean package -DskipTests
```

Expected: BUILD SUCCESS。

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/joker/ai/exam/controller src/main/java/com/joker/ai/exam/service/impl
git commit -m "refactor: 日志注解统一为 @Slf4j（移除未引入的 log4j2 API）"
```

---

### Task 7: 统一异常、错误码与参数校验

**Files:**
- Create: `src/main/java/com/joker/ai/exam/common/ErrorCode.java`
- Create: `src/main/java/com/joker/ai/exam/common/BizException.java`
- Modify: `src/main/java/com/joker/ai/exam/config/GlobalExceptionHandler.java`
- Modify: `src/main/java/com/joker/ai/exam/controller/ExamController.java`
- Test: `src/test/java/com/joker/ai/exam/common/ResultTest.java`
- Test: `src/test/java/com/joker/ai/exam/config/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `Result`（现有 `success/error` 静态方法）、`StartExamVo`（已含 `@NotNull/@NotBlank`）。
- Produces:
  - `ErrorCode` 枚举：`SUCCESS/PARAM_ERROR/UNAUTHORIZED/FORBIDDEN/NOT_FOUND/BUSINESS_ERROR/SYSTEM_ERROR`，方法 `getCode():int`、`getMessage():String`。
  - `BizException extends RuntimeException`，构造 `(String)`、`(ErrorCode)`、`(ErrorCode,String)`，方法 `getErrorCode():ErrorCode`。
  - `GlobalExceptionHandler`：`handleBizException(BizException)`、`handleValidationException(BindException)`、`handleException(Exception)`，均返回 `Result<Void>`。

- [ ] **Step 1: 写 ErrorCode 单测（先失败）**

创建 `src/test/java/com/joker/ai/exam/common/ResultTest.java`：

```java
package com.joker.ai.exam.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultTest {

    @Test
    void success_withData() {
        Result<String> r = Result.success("x");
        assertEquals(200, r.getCode());
        assertEquals("操作成功", r.getMessage());
        assertEquals("x", r.getData());
    }

    @Test
    void error_withCustomCode() {
        Result<Void> r = Result.error(400, "bad");
        assertEquals(400, r.getCode());
        assertEquals("bad", r.getMessage());
        assertNull(r.getData());
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
export JAVA_HOME=/Users/qyk9527/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home
export PATH="$JAVA_HOME/bin:/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin:$PATH"
mvn -q test -Dtest=ResultTest
```

Expected: FAIL — 因为第 3 步才创建 `ErrorCode`/`BizException`（此时 `ResultTest` 本身可过，
但下一个测试类尚不存在；本步也可直接跳到 Step 3）。若 `ResultTest` 直接通过属正常。

- [ ] **Step 3: 新增 ErrorCode 与 BizException**

`src/main/java/com/joker/ai/exam/common/ErrorCode.java`：

```java
package com.joker.ai.exam.common;

public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(500, "业务处理失败"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
```

`src/main/java/com/joker/ai/exam/common/BizException.java`：

```java
package com.joker.ai.exam.common;

public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(String message) {
        super(message);
        this.errorCode = ErrorCode.BUSINESS_ERROR;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

- [ ] **Step 4: 写异常处理器单测**

创建 `src/test/java/com/joker/ai/exam/config/GlobalExceptionHandlerTest.java`：

```java
package com.joker.ai.exam.config;

import com.joker.ai.exam.common.BizException;
import com.joker.ai.exam.common.ErrorCode;
import com.joker.ai.exam.common.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBizException_returnsCodeAndMessage() {
        Result<Void> result = handler.handleBizException(new BizException(ErrorCode.NOT_FOUND, "试卷不存在"));
        assertEquals(404, result.getCode());
        assertEquals("试卷不存在", result.getMessage());
    }

    @Test
    void handleException_hidesInternalMessage() {
        Result<Void> result = handler.handleException(new IllegalStateException("db down"));
        assertEquals(500, result.getCode());
        assertEquals(ErrorCode.SYSTEM_ERROR.getMessage(), result.getMessage());
    }
}
```

- [ ] **Step 5: 重写 GlobalExceptionHandler**

`src/main/java/com/joker/ai/exam/config/GlobalExceptionHandler.java` 全量替换为：

```java
package com.joker.ai.exam.config;

import com.joker.ai.exam.common.BizException;
import com.joker.ai.exam.common.ErrorCode;
import com.joker.ai.exam.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleValidationException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.PARAM_ERROR.getMessage();
        log.warn("参数校验失败：{}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
```

- [ ] **Step 6: 运行单测确认通过**

```bash
mvn -q test -Dtest=ResultTest,GlobalExceptionHandlerTest
```

Expected: BUILD SUCCESS，2 个测试类共 4 个用例通过。

- [ ] **Step 7: 让参数校验生效**

`ExamController.java` 的 `startExam` 方法入参加 `@Valid`：

```java
    public Result<ExamRecord> startExam(@Valid @RequestBody StartExamVo startExamVo) {
```

并确保文件已导入 `jakarta.validation.Valid`。

- [ ] **Step 8: 验证非法参数返回 400**

```bash
mvn -q clean package -DskipTests
java -jar target/exam_system_online-1.0-SNAPSHOT.jar > /tmp/p1_boot.log 2>&1 &
sleep 30
curl -s -X POST http://localhost:8090/api/exams/start \
  -H 'Content-Type: application/json' -d '{"paperId":null,"studentName":""}'
kill %1
```

Expected: 返回 `{"code":400,"message":"试卷ID不能为空",...}` 或 `考生姓名不能为空`。

- [ ] **Step 9: 提交**

```bash
git add src/main/java/com/joker/ai/exam/common src/main/java/com/joker/ai/exam/config/GlobalExceptionHandler.java \
        src/main/java/com/joker/ai/exam/controller/ExamController.java \
        src/test/java/com/joker/ai/exam/common/ResultTest.java \
        src/test/java/com/joker/ai/exam/config/GlobalExceptionHandlerTest.java
git commit -m "feat: 新增 ErrorCode/BizException 与统一异常处理，参数校验生效"
```

---

### Task 8: 冒烟脚本与阶段验收

**Files:**
- Create: `scripts/smoke.sh`

**Interfaces:**
- Consumes: 8090 端口、构建产物 jar、本机 MySQL/Redis。
- Produces: 可重复运行的端到端冒烟验证，失败即非零退出。

- [ ] **Step 1: 编写冒烟脚本**

创建 `scripts/smoke.sh`：

```bash
#!/usr/bin/env bash
set -uo pipefail

APP_PORT=8090
JAR="target/exam_system_online-1.0-SNAPSHOT.jar"
BASE="http://localhost:${APP_PORT}"
LOG=/tmp/exam_smoke_app.log
PID=""

cleanup() { [ -n "$PID" ] && kill "$PID" 2>/dev/null; }
trap cleanup EXIT

if [ "${1:-}" != "--skip-build" ]; then
  mvn -q clean package -DskipTests || { echo "构建失败"; exit 1; }
fi

[ -f "$JAR" ] || { echo "缺少 $JAR，请先构建"; exit 1; }

java -jar "$JAR" > "$LOG" 2>&1 &
PID=$!

echo "等待应用启动（最多 120s）..."
for i in $(seq 1 60); do
  if curl -s -o /dev/null "${BASE}/api/stats/overview"; then break; fi
  sleep 2
done

fail=0
check() {
  local path="$1"
  local body
  body=$(curl -s "${BASE}${path}")
  if printf '%s' "$body" | grep -q '"code":200'; then
    echo "OK   ${path}"
  else
    echo "FAIL ${path} -> ${body:0:200}"
    fail=1
  fi
}

check /api/questions/list
check /api/papers/list
check /api/categories
check /api/categories/tree
check /api/videos
check /api/videos/popular
check /api/banners/active
check /api/notices/active
check /api/stats/overview
check /api/video-categories/tree

if [ "$fail" -eq 0 ]; then
  echo "SMOKE PASS"
else
  echo "SMOKE FAIL（应用日志：$LOG）"
fi
exit $fail
```

- [ ] **Step 2: 赋权并运行**

```bash
cd /Users/qyk9527/ideaProject/exam_system_online
chmod +x scripts/smoke.sh
export JAVA_HOME=/Users/qyk9527/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home
export PATH="$JAVA_HOME/bin:/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin:$PATH"
./scripts/smoke.sh
```

Expected: 10 行 `OK`，最后 `SMOKE PASS`，退出码 0。

- [ ] **Step 3: 提交脚本**

```bash
git add scripts/smoke.sh
git commit -m "test: 新增端到端冒烟脚本 scripts/smoke.sh"
```

- [ ] **Step 4: 阶段验收清单（逐项确认）**

```bash
cd /Users/qyk9527/ideaProject/exam_system_online
echo "1) 明文密钥:"; git grep -n 'sk-' -- . ':!*.example' || echo "  CLEAN"
echo "2) CrossOrigin 残留:"; grep -rc '@CrossOrigin' src/main/java | grep -v ':0' || echo "  CLEAN"
echo "3) Log4j2 残留:"; grep -rn '@Log4j2' src/main/java || echo "  CLEAN"
echo "4) 分支/标签:"; git branch --show-current; git tag | grep pre-refactor-baseline
echo "5) 基线文件:"; ls -la docs/db/baseline
```

Expected: 1/2/3 均为 `CLEAN`；分支为 `refactor/p1-foundation`；标签存在；基线文件齐全。

- [ ] **Step 5: 撰写 P1 完成报告并提交**

创建 `docs/superpowers/2026-09-30-p1-completion.md`，记录：目标版本对照表、
验收命令与真实输出、遗留事项（`allow-circular-references: true` 待 P4 消除、
`src/test/java/MyBatisGeneratoir.java` 为开发工具非测试、Knife4j/springdoc 兼容性实测结果）。

```bash
git add docs/superpowers/2026-09-30-p1-completion.md
git commit -m "docs: P1 后端底座现代化完成报告"
```

---

## Self-Review

**1. Spec coverage**

| Spec 条目 | 对应任务 |
|---|---|
| P0 git 标签/分支 | 已在前置步骤完成（约束中声明） |
| P0 schema/数据 dump | Task 1 |
| P0 冒烟脚本 | Task 8 |
| P1.1 版本与依赖治理 | Task 2 |
| P1.1 Banner fastjson1 注解 | Task 2 Step 6 |
| P1.2 密钥外置 + profile/local | Task 3 |
| P1.3 Hikari | Task 4 |
| P1.3 CORS | Task 5 |
| P1.3 日志统一 | Task 6 |
| P1.3 Actuator | Task 4 |
| P1.4 统一响应/异常/校验 | Task 7 |
| 验收标准 1–7 | Task 8 验收清单 |

**2. Placeholder scan:** 无 TBD/TODO；所有代码步骤含真实代码或真实命令。

**3. Type consistency:** `ErrorCode.getCode()/getMessage()`、`BizException.getErrorCode()`、
`Result.error(Integer,String)`、`GlobalExceptionHandler` 三个方法签名在任务与测试中一致。

**已知偏差说明（需执行者知悉）:**
- Spec 提到「关键 VO 增加校验注解」，实际 `StartExamVo` **已含** `@NotNull/@NotBlank`，
  故 Task 7 只需补 `@Valid`，避免与前端契约冲突；其余 VO 的注解完善归入 P4。
- Spec 提到 profile（dev/local/prod）拆分；本计划用「基础 yml + `application-local.yml`
  覆盖」达到同等效果且改动更小，正式的多 profile 文件拆分归入 P4。
