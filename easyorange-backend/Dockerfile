# =============================================================================
# EasyOrange Backend - 多阶段构建
# =============================================================================
# 阶段:
#   1. builder  : 编译 + repackage 生成 Spring Boot fat jar
#   2. runtime  : 仅 JRE 基础镜像, 启动时运行优化后的 JVM 参数
#
# 构建命令:
#   cd easyorange-backend
#   DOCKER_BUILDKIT=1 docker build -f Dockerfile -t easyorange-backend:latest .
# =============================================================================

# ---- Stage 1: builder ----------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# BuildKit cache mount: ~/.m2 跨构建复用, 第一次冷启后增量构建秒级完成
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -pl easyorange-application -am package -DskipTests -q

# ---- Stage 2: runtime ----------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# 时区 (Alpine 3.18+ 支持 TZ 环境变量)
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata

# 非 root 用户
RUN addgroup -S easyorange && adduser -S easyorange -G easyorange

# 拉取 fat jar
COPY --from=builder --chown=easyorange:easyorange /build/easyorange-application/target/easyorange-application-*.jar app.jar
USER easyorange

# ---- JVM 启动调优 --------------------------------------------------------
# • -Xms256m -Xmx2g        : 堆大小, 避免启动期扩容
# • G1GC                   : JDK 25 默认, 启动期性能稳定, GC 暂停 < 200ms
# • UseStringDeduplication : G1 字符串去重, 节省堆内存
# • sun-misc-unsafe-memory-access=allow : 抑制 JDK 25 对 Lombok Permit 的废弃警告
#
# ⚠ 不要加 -XX:TieredStopAtLevel=1: 实测 Spring 业务代码路径依赖 C2 JIT,
#   关掉 C2 反而让启动慢 25% (7.5s vs 6.1s),得不偿失。
# ----------------------------------------------------------------------------
ENV JAVA_OPTS="-Xms256m -Xmx2g \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    --sun-misc-unsafe-memory-access=allow"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# 启动期 60s: 优化后冷启动 60s 内能完成 (健康检查用 wget, Alpine 自带, 无需额外安装 curl)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java \$JAVA_OPTS -jar app.jar"]
