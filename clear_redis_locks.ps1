# ====================================================================
# Redis 登录锁定清理脚本
# 用途：清除 Redis 中存储的所有登录失败尝试记录
# ====================================================================

Write-Host "============================================================"
Write-Host " 清除 Redis 登录锁定"
Write-Host "============================================================"
Write-Host ""

$containerName = "easyorange-redis"

# 检查 Redis 容器是否运行
$running = docker ps --filter "name=$containerName" --format "{{.Names}}" | Where-Object { $_ -eq $containerName }
if (-not $running) {
    Write-Host "[ERROR] Redis 容器未运行！" -ForegroundColor Red
    Write-Host "请先启动: docker-compose up -d redis"
    Read-Host "按 Enter 退出"
    exit 1
}

Write-Host "[INFO] 正在获取登录尝试记录..." -ForegroundColor Cyan

# 获取所有登录尝试的 key
$keys = docker exec $containerName redis-cli KEYS "eo:user:login:attempts:*"

if ($keys) {
    Write-Host "[INFO] 找到以下登录锁定:" -ForegroundColor Cyan
    Write-Host $keys

    Write-Host ""
    Write-Host "[INFO] 正在清除..." -ForegroundColor Cyan

    # 逐个删除
    $keys -split "`n" | ForEach-Object {
        $key = $_.Trim()
        if ($key) {
            docker exec $containerName redis-cli DEL $key | Out-Null
        }
    }

    Write-Host "[OK] 登录锁定已清除！" -ForegroundColor Green
} else {
    Write-Host "[OK] 没有找到登录锁定记录" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Yellow
Write-Host " 后续步骤:" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Yellow
Write-Host "1. 执行密码重置 SQL:"
Write-Host "   emergency_password_reset.sql"
Write-Host ""
Write-Host "2. 重启 Spring Boot 应用"
Write-Host ""
Write-Host "3. 使用以下凭据登录:"
Write-Host "   用户: admin"
Write-Host "   密码: Password123"
Write-Host ""
Read-Host "按 Enter 退出"
