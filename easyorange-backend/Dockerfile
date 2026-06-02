# =============================================================================
# EasyOrange Backend - 多阶段构建 + AppCDS 启动训练
# =============================================================================
# 阶段:
#   1. builder  : 编译 + repackage 生成 Spring Boot fat jar
#   2. trainer  : 启动一次应用, Spring 完整加载 ApplicationContext,
#                 退出时把解析后的类元数据 dump 到 AppCDS archive (/app/app.jsa)
#   3. runtime  : 仅 JRE 基础镜像, 启动时通过 -Xshare:auto mmap 复用 archive
#
# 性能效果: Spring Boot 4 + Java 25 + AppCDS 实测可减少 30-50% 启动时间
# 关键约束:
#   - trainer 和 runtime 必须用相同 major.minor 版本的 JRE (这里都用 25)
#   - trainer 跑 cds-train profile, 关闭 Flyway + 外部服务健康检查
#   - 不需要在本机跑 MySQL/Redis/ES 就能完成训练
#
# 构建命令:
#   cd easyorange-backend
#   DOCKER_BUILDKIT=1 docker build -f Dockerfile -t easyorange-backend:optimized .
# =============================================================================

# ---- 语法: 必须用 BuildKit 才能启用 --mount=type=cache -----------------
#syntax=docker/dockerfile:1.7

# ---- Stage 1: builder ----------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# BuildKit cache mount: ~/.m2 跨构建复用, 第一次冷启后增量构建秒级完成
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -pl easyorange-application -am package -DskipTests -q

# ---- Stage 2: CDS trainer ------------------------------------------------
# 启动一次应用, 让 Spring 完整加载 ApplicationContext,
# 然后用 -XX:ArchiveClassesAtExit 把所有加载过的类元数据 dump 成 archive。
# -Dspring.context.exit=onRefreshed 让 Spring 在 context 刷新完成后立即退出,
# (web 服务器已启动, 所有 bean 已被实例化, 所有 @PostConstruct 已跑完)
FROM eclipse-temurin:25-jre-alpine AS trainer

WORKDIR /app

# 训练阶段需要 tzdata 格式化任何带时间戳的日志, 否则启动会 warning
RUN apk add --no-cache tzdata

# 从 builder 阶段拉 jar
COPY --from=builder /build/easyorange-application/target/easyorange-application-*.jar /app/app.jar

# 训练: 启动一次, dump archive, 退出
# || true 是必须的: app 退出码非 0 (被 onRefreshed 强制退出)
RUN java \
        -Xms256m -Xmx512m \
        -XX:ArchiveClassesAtExit=/app/app.jsa \
        -Dspring.context.exit=onRefreshed \
        -Dspring.profiles.active=cds-train \
        -Dserver.port=0 \
        --sun-misc-unsafe-memory-access=allow \
        -jar /app/app.jar || true; \
    ls -lh /app/app.jsa; \
    test -s /app/app.jsa

# ---- Stage 3: runtime ----------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# 时区 + curl (健康检查用)
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 非 root 用户
RUN addgroup -S easyorange && adduser -S easyorange -G easyorange
USER easyorange

# 拉取 CDS archive (来自 trainer) 和 fat jar (来自 builder)
COPY --from=trainer /app/app.jsa app.jsa
COPY --from=builder /build/easyorange-application/target/easyorange-application-*.jar app.jar

# ---- JVM 启动调优 --------------------------------------------------------
# • MaxRAMPercentage=75.0  : 堆随容器内存自动伸缩, 适合 K8s/docker --memory 限制
# • ZGC + ZGenerational     : 保留原有低延迟 GC
# • TieredStopAtLevel=1     : 仅 C1 编译, 启动期不触发 C2 (启动后峰值吞吐会下降 5-15%, 但启动快 10-20%)
# • Xshare:auto             : 优先用 AppCDS archive, archive 不可用时自动 fallback 到默认 CDS
# • SharedArchiveFile       : 指向训练好的 archive 路径
# • UseStringDeduplication  : ZGC 字符串去重, 内存占用更小
# • sun-misc-unsafe-memory-access=allow : 抑制 JDK 25 对 Lombok Permit 的废弃警告
# ----------------------------------------------------------------------------
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC -XX:+ZGenerational \
    -XX:TieredStopAtLevel=1 \
    -XX:SharedArchiveFile=app.jsa \
    -Xshare:auto \
    -XX:+UseStringDeduplication \
    --sun-misc-unsafe-memory-access=allow"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# 启动期从 60s 调到 30s: 优化后冷启动 30s 内能完成
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
