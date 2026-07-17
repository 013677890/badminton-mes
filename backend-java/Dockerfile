# ----------------------------------------------------------------------------
# 羽毛球 MES 后端镜像(多阶段构建)
#
# 构建：docker build -t badminton-mes/backend:dev .
# 说明：构建阶段使用官方 Gradle 镜像，项目已迁移为 Gradle + JPA。
# ----------------------------------------------------------------------------

# Docker Hub 代理前缀；Compose 可通过 MES_DOCKER_MIRROR 覆盖。
ARG DOCKER_MIRROR=docker.m.daocloud.io/library
ARG GRADLE_MAVEN_MIRROR=https://maven.aliyun.com/repository/public

# ---------- 构建阶段：编译并按 Spring Boot 分层拆解 fat jar ----------
# 使用 Gradle Wrapper 同版本 + JDK 21，与本地构建保持一致
FROM ${DOCKER_MIRROR}/gradle:9.3.1-jdk21 AS builder
ARG GRADLE_MAVEN_MIRROR
WORKDIR /workspace
ENV GRADLE_MAVEN_MIRROR=${GRADLE_MAVEN_MIRROR}

# 先复制 Gradle 配置预拉依赖：构建文件未变时命中缓存层，源码改动不触发依赖下载
COPY settings.gradle build.gradle ./
RUN gradle --no-daemon dependencies

COPY src ./src
# 镜像构建阶段跳过测试，单测在 CI 阶段独立执行并作为准入(TEST-006)
RUN gradle --no-daemon bootJar -x test

# Boot 4 tools 模式分层解包：dependencies(变动最少) → loader → snapshot → application(变动最多)，
# 运行阶段按此顺序 COPY，业务代码改动只重建最薄的应用层
RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination extracted

# ---------- 运行阶段：仅含 JRE 与应用分层 ----------
FROM ${DOCKER_MIRROR}/eclipse-temurin:21-jre AS runtime

# 时区统一为业务时区：日志时间戳与 MySQL NOW() 写入的 update_time 对齐
ENV TZ=Asia/Shanghai

# 容器感知的 JVM 参数：
# - Initial/MaxRAMPercentage 等值，按容器内存配额取百分比，等价于 Xms=Xmx 防止 GC 后
#   调整堆大小带来的压力(SERVER-006)；余量留给元空间/线程栈/直接内存
# - OOM 时输出堆转储便于事后定位(SERVER-005)
ENV JAVA_OPTS="-XX:InitialRAMPercentage=60.0 -XX:MaxRAMPercentage=60.0 \
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/dumps"

WORKDIR /app

# 非 root 运行(安全规约：最小权限)；dumps 目录供 OOM 堆转储写入
RUN useradd --system --create-home --shell /usr/sbin/nologin spring \
    && mkdir -p /app/dumps \
    && chown -R spring:spring /app
USER spring

# 依赖层在前、应用层在后，充分利用镜像层缓存
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

EXPOSE 8080

# sh -c 以展开 JAVA_OPTS；exec 使 java 成为 1 号进程，正确接收停止信号优雅停机
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
