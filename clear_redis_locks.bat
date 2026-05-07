@echo off
REM ====================================================================
REM Redis 登录锁定清理脚本
REM 用途：清除 Redis 中存储的所有登录失败尝试记录
REM       解锁被锁定的账户
REM ====================================================================

echo ============================================================
echo  清除 Redis 登录锁定
echo ============================================================
echo.

docker exec easyorange-redis redis-cli KEYS "eo:user:login:attempts:*"
echo ============================================================
echo  正在清除以上所有登录锁定...
echo.

docker exec easyorange-redis redis-cli --scan --pattern "eo:user:login:attempts:*" | while read key; do docker exec easyorange-redis redis-cli DEL "$key"; done

echo.
echo [OK] 登录锁定已清除！
echo.
echo 现在请执行 SQL 脚本修复密码：
echo   emergency_password_reset.sql
echo.
echo 然后重启应用以重新加载数据。
echo.
pause
