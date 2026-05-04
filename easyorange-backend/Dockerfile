# EasyOrange Backend - 多阶段构建
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build
COPY . .

RUN ./mvnw clean package -DskipTests -q

# 运行阶段
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户
RUN addgroup -S easyorange && adduser -S easyorange -G easyorange
USER easyorange

COPY --from=builder /build/easyorange-application/target/easyorange-application-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseZGC -XX:+ZGenerational"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
