# WebSocket 项目 - 仅启动后端

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  启动后端服务" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path "pom.xml")) {
    Write-Host "错误: 请在项目根目录运行此脚本！" -ForegroundColor Red
    exit 1
}

Write-Host "正在启动后端服务..." -ForegroundColor Yellow
Write-Host ""
Write-Host "后端地址: http://localhost:8080" -ForegroundColor Cyan
Write-Host "WebSocket端点: ws://localhost:8080/ws" -ForegroundColor Cyan
Write-Host "测试页面: http://localhost:8080/test.html" -ForegroundColor Cyan
Write-Host ""

.\mvnw.cmd spring-boot:run
