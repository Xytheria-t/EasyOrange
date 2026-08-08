#!/bin/bash
# =============================================================================
# 生成 JWT RSA 密钥对 → demo overlay 的 env/jwt/ 目录（已被 gitignore，勿提交）
# 用法: scripts/generate-jwt-keys.sh
# 输出: k8s/overlays/demo/env/jwt/private.pem + public.pem（PKCS#8）
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="${SCRIPT_DIR}/../overlays/demo/env/jwt"

mkdir -p "$OUT_DIR"
bash "${SCRIPT_DIR}/../../easyorange-backend/keys/generate-rsa-keypair.sh" "$OUT_DIR"

echo ">>> JWT 密钥已生成: $OUT_DIR"
echo ">>> 警告: 该目录已 gitignore，请妥善保管私钥；生产环境应使用 KMS/ExternalSecret"
