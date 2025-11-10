# WebSocket 项目 - 仅启动前端

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  启动前端服务" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path "webSocketFront/package.json")) {
    Write-Host "错误: 请在项目根目录运行此脚本！" -ForegroundColor Red
    exit 1
}

Write-Host "正在启动前端服务..." -ForegroundColor Yellow
Write-Host ""
Write-Host "前端地址: http://localhost:5173" -ForegroundColor Cyan
Write-Host ""
Write-Host "提示: 请确保后端服务已启动（http://localhost:8080）" -ForegroundColor Yellow
Write-Host ""

cd webSocketFront
npm run dev
