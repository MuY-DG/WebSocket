# WebSocket 项目启动脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  WebSocket 实时通信应用启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查是否在正确的目录
if (-not (Test-Path "pom.xml")) {
    Write-Host "错误: 请在项目根目录运行此脚本！" -ForegroundColor Red
    exit 1
}

# 启动后端
Write-Host "[1/2] 启动后端服务..." -ForegroundColor Yellow
Write-Host "      后端地址: http://localhost:8080" -ForegroundColor Gray
Write-Host "      测试页面: http://localhost:8080/test.html" -ForegroundColor Gray
Write-Host ""

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; .\mvnw.cmd spring-boot:run"

# 等待后端启动
Write-Host "等待后端服务启动（10秒）..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 启动前端
Write-Host "[2/2] 启动前端服务..." -ForegroundColor Yellow
Write-Host "      前端地址: http://localhost:5173" -ForegroundColor Gray
Write-Host ""

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD\webSocketFront'; npm run dev"

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  启动完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📱 前端应用: http://localhost:5173" -ForegroundColor Cyan
Write-Host "🔧 后端API:  http://localhost:8080" -ForegroundColor Cyan
Write-Host "🧪 测试工具: http://localhost:8080/test.html" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 使用说明:" -ForegroundColor Yellow
Write-Host "   1. 打开多个浏览器窗口访问前端地址" -ForegroundColor Gray
Write-Host "   2. 使用不同的用户名登录" -ForegroundColor Gray
Write-Host "   3. 开始聊天和发送通知！" -ForegroundColor Gray
Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
