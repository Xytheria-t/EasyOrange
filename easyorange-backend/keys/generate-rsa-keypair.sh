#!/bin/bash
# =============================================================================
# 生成 RSA 密钥对（PEM 格式）用于 JWT 签名
# =============================================================================
# 说明：
#   生产环境部署前需运行此脚本生成 RSA 密钥对，并将密钥文件路径配置到
#   JWT_RSA_PRIVATE_KEY 和 JWT_RSA_PUBLIC_KEY 环境变量。
#
# 用法：
#   ./keys/generate-rsa-keypair.sh [output-dir]
#
#   默认输出到当前目录下的 keys/ 目录。
# =============================================================================

set -euo pipefail

OUTPUT_DIR="${1:-keys}"
PRIVATE_KEY="${OUTPUT_DIR}/private.pem"
PUBLIC_KEY="${OUTPUT_DIR}/public.pem"

mkdir -p "$OUTPUT_DIR"

echo ">>> 生成 2048 位 RSA 密钥对..."

# 生成私钥
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIVATE_KEY" 2>/dev/null
# 提取公钥
openssl pkey -in "$PRIVATE_KEY" -pubout -out "$PUBLIC_KEY" 2>/dev/null

# 转换为 PKCS#8 格式（Java 标准格式）
openssl pkcs8 -topk8 -inform PEM -outform PEM -in "$PRIVATE_KEY" -out "${PRIVATE_KEY}.tmp" -nocrypt 2>/dev/null
mv "${PRIVATE_KEY}.tmp" "$PRIVATE_KEY"

echo ">>> 密钥对已生成："
echo "    Private key: ${PRIVATE_KEY}"
echo "    Public key:  ${PUBLIC_KEY}"
echo ""
echo ">>> 配置环境变量："
echo "    export JWT_RSA_PRIVATE_KEY=$(pwd)/${PRIVATE_KEY}"
echo "    export JWT_RSA_PUBLIC_KEY=$(pwd)/${PUBLIC_KEY}"
echo ""
echo ">>> 或直接复制到目标目录，然后配置："
echo "    jwt.private-key-location: /path/to/private.pem"
echo "    jwt.public-key-location: /path/to/public.pem"