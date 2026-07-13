# 羽毛球 MES 项目技术栈约束

适用：羽毛球 MES 系统前后端框架构建、依赖选型、运行时版本、数据库基线、工程脚手架与后续 Agent 生成代码。

本文件是项目级约束，优先用于补充《Java开发手册（黄山版）》规则卡片。若用户后续明确指定不同版本或技术路线，以用户最新指令为准，并同步更新本文件。

规则格式：`ID` **等级** 可执行要求；说明用于解释取舍和兼容边界。

## 核心版本锁定

以下版本作为当前项目框架构建的默认稳定基线。对 JDK、Node.js、pnpm 这类会持续发布安全补丁的运行时或工具，锁定主版本/LTS 线，安装时使用该主线下最新稳定补丁版本。


| 技术项                | 项目基线版本               | 约束说明                                  |
| ------------------ | -------------------- | ------------------------------------- |
| JDK                | 21 LTS               | 使用 JDK 21 最新稳定补丁版本，例如 21.0.x。         |
| Java 语言级别          | 21                   | `source`、`target`、`release` 统一为 21。   |
| Spring Boot        | 4.0.7                | 采用 Spring Boot 4.0.x稳定主线，初始基线为 4.0.7。 |
| Spring Framework   | 由 Spring Boot BOM 管理 | 不手动覆盖 Spring Framework 版本。            |
| Maven              | 3.9.16               | 通过 Maven Wrapper 固定构建工具版本。            |
| Vue                | 3.5.39               | 采用 Vue 3 当前稳定主线。                      |
| Vite               | 8.1.3                | 采用 Vite 当前稳定主线。                       |
| @vitejs/plugin-vue | 6.0.7                | 与 Vite 8 和 Vue 3 配套。                  |
| TypeScript         | 6.0.3                | 前端工程使用 TypeScript 当前稳定版，不使用 beta/rc。  |
| Node.js            | 24 LTS               | 使用 Node.js 24 最新稳定补丁版本。               |
| pnpm               | 11.10.0              | 通过 Corepack 或 `packageManager` 字段固定。  |
| Vue Router         | 4.6.4                | 采用 Vue Router 4.x 成熟稳定线；不默认升级到 5.x。   |
| Pinia              | 3.0.4                | 作为默认状态管理方案。                           |
| Element Plus       | 2.14.2               | 管理后台默认 UI 组件库。                        |
| ECharts            | 6.1.0                | 图表按需引入。                               |
| MySQL              | 8.4 LTS              | 默认数据库基线，兼容 MySQL 8.0 部署环境。            |


## 一、总体基线

- `STACK-001` **强制** 后端采用 Spring Boot 4.x、前端采用 Vue 3、数据库采用 MySQL，除非用户明确要求变更技术路线。
  - 说明：这是羽毛球 MES 系统当前框架构建的基础约束，后续 Agent 生成代码、脚手架和依赖配置时必须先遵守该基线。
- `STACK-002` **强制** 版本号必须集中管理，不允许在多模块或多配置文件中散落重复定义同一依赖族版本。
  - 说明：后端优先使用 Spring Boot BOM 管理 Spring 生态依赖版本；前端使用锁文件固定依赖解析结果；数据库脚本使用版本化迁移管理。
- `STACK-003` **推荐** 未经确认不要追逐非稳定版本；框架和依赖优先选择当前稳定版、LTS 版或 Spring Boot BOM 管理版本。
  - 说明：教学实训项目也需要可复现构建，避免由于 snapshot、beta、rc 版本导致框架搭建不稳定。

## 二、后端技术栈

- `BACKEND-001` **强制** JDK 使用 21 LTS 最新稳定补丁版本，Java 语言级别、`source`、`target`、`release` 均使用 21。
  - 说明：Spring Boot 4.x 的最低 Java 基线为 17+，本项目统一选择 Java 21 LTS，以兼顾长期支持、虚拟线程等现代 JVM 能力和生态稳定性。
  - 约束：默认不启用 Java preview features；如需启用预览特性，必须先获得用户确认并在构建配置中显式声明。
- `BACKEND-002` **强制** Spring Boot 使用 4.0.x 稳定主线，项目初始基线版本为 4.0.7；Spring Framework、Jackson、Tomcat/Jetty/Undertow、Logback、JUnit 等 Spring 生态依赖默认由 Spring Boot BOM 仲裁。
  - 说明：不要手动混用 Spring Framework 6.x/7.x 或不同 Spring Boot 大版本依赖，避免运行期类冲突和 Jakarta EE API 不兼容。
- `BACKEND-003` **强制** 后端 Web/API 使用 Jakarta 命名空间，不再使用 `javax.`* 旧包名。
  - 说明：Spring Boot 4 基于 Jakarta EE 新基线，Servlet、Validation、Persistence 等相关 API 应使用 `jakarta.`*。
- `BACKEND-004` **推荐** 构建工具优先使用 Maven Wrapper，并采用 Maven 3.9.x 或更高稳定版本。
  - 说明：如果项目后续改用 Gradle，需要同步写明 Gradle Wrapper 版本和 Java toolchain 配置。
- `BACKEND-005` **推荐** 后端基础依赖默认包括 Spring Web、Spring Validation、Spring AOP、Spring JDBC 或 ORM 适配、MySQL Connector/J、测试 Starter、日志门面与必要的配置处理器。
  - 说明：具体 ORM 方案可在详细设计阶段确认；未确认前不要擅自引入重量级中间件。
- `BACKEND-006` **推荐** 对外 REST API 统一返回结构、错误码和分页模型，并遵守 `java-control-comments-api.md` 与 `error-exception-logging.md`。
  - 说明：前后端分离框架搭建时应先定义接口契约，避免后续 Vue 页面和 Spring Controller 反复返工。
- `BACKEND-007` **推荐** Java 21 可以使用虚拟线程；阻塞 I/O 较多、并发等待较多的接口或后台任务可以评估启用虚拟线程。
  - 说明：虚拟线程在 Java 21 中已经正式发布，不是 preview feature，不需要额外开启预览参数。
  - Spring Boot 约束：如需在 Spring Boot Web 应用中启用虚拟线程，可优先通过配置项 `spring.threads.virtual.enabled=true` 开启，并在压测后决定是否作为默认配置。
  - 使用边界：虚拟线程适合提升阻塞等待型任务的并发承载能力，不会让 CPU 密集型计算变快；使用 JDBC/MySQL 时仍必须通过连接池限制数据库并发，不能把虚拟线程数量等同于数据库连接数。
  - 风险提示：启用前需要关注 `synchronized` 临界区、ThreadLocal 使用、本地方法调用、长事务和监控链路，避免出现线程 pinning 或上下文泄露问题。

### Java 21 虚拟线程启用示例

后续 Agent 为后端生成配置时，优先使用 Spring Boot 配置项启用虚拟线程，不要在业务代码中零散创建线程。

`application.yml` 推荐写法：

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

`application.properties` 等价写法：

```properties
spring.threads.virtual.enabled=true
```

如果后续使用 JDBC/MySQL，必须同时通过连接池限制数据库连接数，避免虚拟线程放大数据库并发压力。示例：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

仅当需要为 `@Async` 任务自定义命名、隔离或监控时，才显式声明虚拟线程执行器。示例：

```java
package com.badminton.mes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncExecutorConfiguration {

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new VirtualThreadTaskExecutor("badminton-mes-virtual-");
    }
}
```

使用约束：

- 默认优先使用 `spring.threads.virtual.enabled=true`，只有存在明确隔离需求时再增加 Java 配置类。
- 不要把虚拟线程数量等同于下游资源容量；数据库连接池、远程接口超时时间和限流策略仍必须显式配置。
- 不要在虚拟线程任务中长期持有锁或依赖未清理的 `ThreadLocal` 上下文。
- 对 CPU 密集型任务继续使用有界平台线程池或专用调度方案，不要期望虚拟线程提升计算性能。

## 三、前端技术栈

- `FRONTEND-001` **强制** 前端采用 Vue 3，默认使用 Composition API 和 `<script setup>` 组织组件逻辑。
  - 说明：不再使用 Vue 2、Options API 作为默认写法，除非维护已有代码或用户明确要求。
- `FRONTEND-002` **强制** Node.js 使用 24 LTS 最新稳定补丁版本作为默认开发运行基线；如本机环境暂不满足，最低不得低于当前 Vite 稳定版要求的 Node.js LTS 版本。
  - 说明：本项目建议用 LTS 版本保证依赖安装和构建行为稳定；不要使用已 EOL 的 Node.js 版本。
- `FRONTEND-003` **推荐** 前端核心版本使用 Vue 3.5.39、Vite 8.1.3、@vitejs/plugin-vue 6.0.7、TypeScript 6.0.3、Pinia 3.0.4、Vue Router 4.6.4。
  - 说明：Vue 3 官方生态已将 Vite、Pinia、Vue Router 4 作为主流组合，适合管理后台和 MES 前端模块化开发。
- `FRONTEND-004` **推荐** 包管理器优先使用 pnpm 11.10.0，并提交锁文件保证安装可复现。
  - 说明：如果教学环境统一使用 npm，可以改用 npm，但必须保留对应 lockfile，不能混用多个包管理器锁文件。
- `FRONTEND-005` **推荐** 管理后台 UI 组件库默认选择 Element Plus 2.14.2；图表按需使用 ECharts 6.1.0。
  - 说明：羽毛球 MES 系统包含订单、生产、库存、工资等后台管理场景，Element Plus 能降低表单、表格、弹窗和导航布局成本。

## 四、数据库与数据访问

- `DATABASE-001` **强制** MySQL 使用 8.4 LTS 作为默认数据库版本；如部署环境只能提供 MySQL 8.0，必须保证 SQL、字符集、索引和时间类型兼容。
  - 说明：本项目不以 MySQL 5.7 或更早版本作为兼容目标。
- `DATABASE-002` **强制** 数据库默认使用 InnoDB、`utf8mb4` 字符集和 `utf8mb4_0900_ai_ci` 或项目统一指定的排序规则。
  - 说明：涉及中文、英文、符号和可能的 emoji 内容时，`utf8mb4` 是默认安全选择。
- `DATABASE-003` **强制** 表结构、字段、索引、SQL 和 ORM 映射必须遵守 `mysql.md`；所有正式表必须有主键、可解释字段类型和必要索引。
  - 说明：MES 业务数据具有较强追溯性，表设计阶段必须考虑订单、批次、工序、库存和工资数据的关联关系。
- `DATABASE-004` **推荐** 数据库结构变更使用版本化迁移脚本管理，例如 Flyway 或等价方案。
  - 说明：如果暂不引入迁移工具，也必须按版本保存 SQL 脚本，避免只依赖手工执行记录。

## 五、后续 Agent 执行要求

- `AGENT-001` **强制** 任何涉及脚手架、依赖、构建配置、Java 代码、Vue 代码或 MySQL 设计的任务，必须先读取本文件再开始实现。
- `AGENT-002` **强制** 如果用户要求的版本与本文件冲突，先按用户要求执行，并在完成时提示是否需要同步更新本约束。
- `AGENT-003` **推荐** 生成项目骨架时同步输出版本说明，例如 README、后端 `pom.xml` 属性、前端 `package.json` engines 字段和数据库初始化说明。

