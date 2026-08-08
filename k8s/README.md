# EasyOrange Kubernetes 部署

> K8s 的定位：**无状态应用层（backend + frontend）的生产级编排**。本地开发/集成测试仍走 docker compose（三件套栈），互不替代。

## 架构总览

```
                        ┌─ Ingress (traefik, k3s 内置) ─┐
                        │   easyorange.local              │
                        └───────┬─────────────┬──────────┘
                        /api/ /ws/          / (静态站)
                        │                  │
               ┌────────▼─────────┐  ┌─────▼──────────────┐
               │ easyorange-app   │  │ easyorange-frontend│
               │ (Service 8080)   │  │ nginx (Service 80) │
               │ Deployment ×2-6  │  │ Deployment ×2      │
               │ HPA + PDB + 探针 │  │ (静态 + 内部反代)   │
               └───┬───┬───┬──────┘  └────────────────────┘
                   │   │   │   └─► 公网 443（LLM/DashScope）
              mysql│ redis│ rabbitmq│
          (StatefulSet demo-only，生产换托管服务)
```

**目录结构**（kustomize：base 可复用 + overlay 按环境差异化）：

```
k8s/
├── base/                    # 无状态应用层（可被任意环境引用，不直接 apply）
│   ├── namespace.yaml       # Pod Security: enforce=baseline / warn=restricted
│   ├── configmap.yaml       # 非敏感配置（prod profile + 中间件地址）
│   ├── backend/             # Deployment + Service + HPA + PDB + PVC(uploads)
│   ├── frontend/            # nginx Deployment + Service
│   ├── ingress.yaml         # /api /ws → backend；/ → frontend
│   └── network-policy.yaml  # 零信任：默认拒绝 + 显式放行
├── overlays/
│   └── demo/                # k3s 单机自包含演示（含 infra 中间件）
│       ├── env/             # 敏感配置（gitignored，见 *.example）
│       ├── infra/           # MySQL/Redis/RabbitMQ StatefulSet（demo-only）
│       └── patch-configmap.yaml  # HTTP 演示覆盖项
├── observability/           # ServiceMonitor + PrometheusRule（需 kube-prometheus-stack）
└── scripts/                 # JWT 密钥生成脚本
```

## 现代化最佳实践清单

| 实践 | 落地位置 |
|------|---------|
| 零中断滚动发布 | `maxUnavailable=0` + `maxSurge=1` + 优雅停机（`terminationGracePeriodSeconds=120` > 90s drain） |
| 探针三件套 | startup=`/actuator/health`（360s 冷启动窗口）/ readiness=`/actuator/health/readiness`（DB/Redis/Rabbit 就绪）/ liveness=`/actuator/health/liveness`（仅 JVM，中间件抖动不误杀） |
| 水平弹性 | HPA CPU 70% 基准，2→6 副本，缩容稳定窗口 300s；**内存不进 HPA**（Java GC 会掩盖真实压力） |
| 故障容忍 | PDB `minAvailable=1`，节点 drain 不中断服务 |
| 不可变基础设施 | 只读根文件系统 + emptyDir（logs/tmp）+ PVC（uploads） |
| 最小权限 | 非 root（backend 1000 / nginx 101）、drop ALL cap、seccomp RuntimeDefault、`automountServiceAccountToken=false` |
| 配置分层 | 非敏感→ConfigMap；敏感→Secret（kustomize secretGenerator，内容 gitignore）；JWT PEM→Secret 只读挂载 |
| 零信任网络 | 默认拒绝 + 按组件显式放行（backend→mysql/redis/rabbitmq/443；仅 traefik/prometheus 可进 8080） |
| 可观测性 | ServiceMonitor 抓 `/actuator/prometheus` + 3 条业务告警（Down/5xx/P99） |
| Pod Security | 命名空间 enforce=baseline（infra 以 root 运行），backend/frontend 满足 restricted |

## k3s 快速部署（demo）

前置：k3s 单机 + kubectl；`docker` 构建镜像；`kubectl get sc` 应存在默认 StorageClass（k3s 自带 `local-path`）。

```bash
# 1. 构建镜像（复用现有 Dockerfile）
cd easyorange-backend && docker build -t easyorange-backend:local . && cd ..
cd easyorange-frontend && docker build -t easyorange-frontend:local . && cd ..

# 2. 导入镜像到 k3s（单节点；多节点用 registry 或 k3s airgap 导入）
docker save easyorange-backend:local easyorange-frontend:local | sudo k3s ctr images import -

# 3. 准备敏感配置（生成 RSA 密钥对 + 填写密码）
bash k8s/scripts/generate-jwt-keys.sh
cp k8s/overlays/demo/env/backend.env.example k8s/overlays/demo/env/backend.env
cp k8s/overlays/demo/infra/env/infra.env.example k8s/overlays/demo/infra/env/infra.env
# 编辑 backend.env / infra.env 填写真实密码与 AI key（MYSQL_PASSWORD 必须与 EASYORANGE_DB_PASSWORD 一致）

# 4. 部署
kubectl apply -k k8s/overlays/demo

# 5. 等待就绪
kubectl -n easyorange rollout status deploy/easyorange-backend --timeout=300s
kubectl -n easyorange get pods

# 6. 本地访问（/etc/hosts 增加: 127.0.0.1 easyorange.local）
curl -H "Host: easyorange.local" http://<k3s-node-ip>/actuator/health
# 浏览器打开 http://easyorange.local（k3s 本机则直接 http://easyorange.local）

# 7. 验证 HPA（可选压测）
kubectl -n easyorange get hpa -w
# 可选: rabbitmq management 端口转发 kubectl -n easyorange port-forward svc/rabbitmq 15672:15672
```

**回滚/清理**：

```bash
kubectl -n easyorange rollout undo deploy/easyorange-backend
kubectl delete -k k8s/overlays/demo        # 清应用（PVC 保留数据，如需连数据一起删: kubectl delete pvc -n easyorange --all）
```

## 可观测性（可选）

```bash
# 安装 kube-prometheus-stack（如未安装）
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace

# 部署 ServiceMonitor + PrometheusRule（release 标签如不一致需同步修改）
kubectl apply -k k8s/observability
```

Grafana 中可导入 `infra/grafana/provisioning/dashboards/easyorange-dashboard.json`。

## 生产上线前清单（demo → prod）

| 项 | demo 现状 | 生产要求 |
|----|----------|---------|
| 中间件 | 集群内裸 StatefulSet（无 HA/备份） | 托管服务（RDS/ElastiCache/托管 MQ）或 Operator（KubeBlocks/RabbitMQ Cluster Operator）+ 定时备份 |
| 镜像 | 本地 `:local` 标签 | 私有 Registry（ghcr/ECR）+ `imagePullPolicy: Always` + 镜像签名（cosign） |
| TLS | HTTP + `JWT_REFRESH_COOKIE_SECURE=false` 覆盖 | Ingress TLS（cert-manager + ClusterIssuer），**删除该覆盖**，`SPRING_DATASOURCE_URL` 恢复 `useSSL=true`（或托管库自带 TLS） |
| Redis | 集群内无 TLS | `SPRING_DATA_REDIS_SSL=true` 或托管实例 |
| 密钥 | kustomize secretGenerator + gitignore | External Secrets Operator / SealedSecrets / SOPS + KMS 轮换；JWT 私钥 90 天轮换 |
| 上传存储 | RWO PVC（单节点共享） | 多节点需 RWX（NFS/SMB）或对象存储（S3/OSS，接入 `FileStoragePort`） |
| 网络策略 | k3s flannel 不强制 | 换 Cilium/kube-router 后自动生效（策略已就位） |
| 日志 | 容器内 emptyDir | Loki/EFK 采集 + 集中存储 |
| 弹性 | CPU HPA | + KEDA（QPS/队列深度）、拓扑分布 `topologySpreadConstraints`、跨可用区 PDB |
| GitOps（可选） | kubectl apply | ArgoCD/Flux 声明式同步 + 发布审批 |

## 常见问题

- **`ImagePullBackOff`**：镜像未导入 k3s（重跑 `docker save | k3s ctr images import`）或标签不匹配
- **后端 `CrashLoopBackOff` 且日志 `Unable to connect to Redis`**：`easyorange-secrets` 中 `REDIS_PASSWORD` 与 `easyorange-infra-secrets` 不一致（两处需同时改）
- **登录后 refresh cookie 丢失**：demo 为 HTTP，已通过 `JWT_REFRESH_COOKIE_SECURE=false` 覆盖；若删掉该覆盖又未上 HTTPS，会触发此现象
- **探针 401/404**：基础配置已放行 `security.ignore-paths` 中 `/actuator/health/**`（含 readiness/liveness 组，`probes.enabled=true`），且 AI 未配置时 `AiHealthIndicator` 返回 UNKNOWN 不拉低就绪——按 k8s 探针预期工作，无需改动
- **NetworkPolicy 不生效**：k3s 默认 flannel 无策略控制器，需 Cilium/kube-router
