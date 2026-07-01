# EasyOrange Frontend - 多阶段构建

# 构建阶段
FROM node:20-alpine AS builder

WORKDIR /build

COPY package*.json ./
RUN npm ci --silent

COPY . .
RUN npm run build

# 运行阶段
FROM nginx:alpine

# 安装 curl 用于健康检查
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf security-headers.conf /etc/nginx/

# 创建缓存目录
RUN mkdir -p /var/cache/nginx/images && \
    chown -R nginx:nginx /var/cache/nginx

EXPOSE 80 443

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
