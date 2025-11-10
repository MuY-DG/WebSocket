# 故障排查指南

## ❌ 问题 1: `global is not defined` 错误

### 错误信息
```
Uncaught ReferenceError: global is not defined
    at node_modules/sockjs-client/lib/utils/browser-crypto.js
```

### 原因
SockJS Client 使用了 Node.js 的 `global` 对象，但在浏览器环境中不存在。Vite 默认不会自动 polyfill 这个对象。

### ✅ 解决方案

在 `vite.config.ts` 中添加以下配置：

```typescript
export default defineConfig({
  // ... 其他配置
  define: {
    // 修复 SockJS 的 global is not defined 错误
    global: 'globalThis',
  },
})
```

**完整配置文件：**
```typescript
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  define: {
    global: 'globalThis',
  },
})
```

### 重启服务
修改 `vite.config.ts` 后需要重启前端服务：

```powershell
# 按 Ctrl+C 停止当前服务
# 然后重新启动
cd webSocketFront
npm run dev
```

---

## ❌ 问题 2: 页面空白

### 可能原因
1. JavaScript 错误导致应用无法加载
2. WebSocket 连接失败
3. 后端服务未启动

### ✅ 排查步骤

#### 1. 检查浏览器控制台
按 `F12` 打开开发者工具，查看 Console 标签中的错误信息。

#### 2. 检查网络请求
在开发者工具的 Network 标签中：
- 检查是否有红色的失败请求
- 检查 WebSocket 连接状态（WS 过滤器）

#### 3. 检查后端服务
确保后端服务正在运行：
```powershell
# 在另一个终端窗口启动后端
cd d:\project-NEW\Java\WebSocket
.\mvnw.cmd spring-boot:run
```

访问 http://localhost:8080 确认后端是否正常。

#### 4. 清除缓存
有时浏览器缓存会导致问题：
- 按 `Ctrl+Shift+R` 强制刷新
- 或在开发者工具中右键刷新按钮，选择"清空缓存并硬性重新加载"

---

## ❌ 问题 3: WebSocket 连接失败

### 错误信息
```
WebSocket connection to 'ws://localhost:8080/ws' failed
```

### ✅ 解决方案

#### 1. 确认后端已启动
```powershell
.\start-backend.ps1
```

#### 2. 检查端口是否被占用
```powershell
# 检查 8080 端口
netstat -ano | findstr :8080
```

如果端口被占用，修改 `application.properties`:
```properties
server.port=8081  # 改为其他端口
```

然后修改前端 `useWebSocket.ts`:
```typescript
const SOCKET_URL = 'http://localhost:8081/ws'  // 对应后端端口
```

---

## ❌ 问题 4: CORS 跨域错误

### 错误信息
```
Access to XMLHttpRequest has been blocked by CORS policy
```

### ✅ 解决方案

已在 `WebConfig.java` 中配置 CORS，如果还有问题，确认配置：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

## ❌ 问题 5: 编译错误

### TypeScript 类型错误

如果看到 TypeScript 编译错误，确认已安装类型定义：

```powershell
cd webSocketFront
npm install --save-dev @types/sockjs-client
```

### Maven 编译错误

清理并重新编译：
```powershell
.\mvnw.cmd clean install
```

---

## ❌ 问题 6: 依赖安装失败

### npm 安装问题

```powershell
# 清除缓存
npm cache clean --force

# 删除 node_modules 和 package-lock.json
Remove-Item -Recurse -Force node_modules
Remove-Item package-lock.json

# 重新安装
npm install
```

### Maven 依赖问题

```powershell
# 强制更新依赖
.\mvnw.cmd clean install -U
```

---

## 🔧 完整启动检查清单

### 启动前检查
- [ ] Java 17+ 已安装
- [ ] Node.js 20+ 已安装
- [ ] 端口 8080 和 5173 未被占用

### 启动步骤

#### 方式 1: 一键启动（推荐）
```powershell
.\start.ps1
```

#### 方式 2: 分别启动

**终端 1 - 后端：**
```powershell
cd d:\project-NEW\Java\WebSocket
.\mvnw.cmd spring-boot:run
```

等待看到 "Started WebSocketApplication" 后继续。

**终端 2 - 前端：**
```powershell
cd d:\project-NEW\Java\WebSocket\webSocketFront
npm run dev
```

看到 "Local: http://localhost:5173/" 即成功。

### 验证运行

1. **后端验证**
   - 访问 http://localhost:8080
   - 应该看到欢迎页面

2. **前端验证**
   - 访问 http://localhost:5173
   - 应该看到登录页面
   - 不应该有控制台错误

3. **WebSocket 验证**
   - 登录后查看状态指示器
   - 应该显示绿色（已连接）

---

## 🐛 调试技巧

### 1. 启用详细日志

**后端** - 修改 `application.properties`:
```properties
logging.level.org.springframework.web.socket=TRACE
logging.level.org.springframework.messaging=TRACE
```

**前端** - 查看 `useWebSocket.ts` 中的 debug 输出：
```typescript
debug: (str) => {
  console.log('STOMP Debug:', str)  // 已启用
}
```

### 2. 检查 WebSocket 连接

在浏览器开发者工具：
1. 切换到 **Network** 标签
2. 点击 **WS** 过滤器
3. 查看 WebSocket 连接状态和消息

### 3. 测试后端 API

使用测试页面：
```
http://localhost:8080/test.html
```

或使用 curl：
```bash
curl -X POST http://localhost:8080/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{"recipient":"test","type":"INFO","title":"测试","message":"测试消息"}'
```

---

## 📞 常见问题快速索引

| 问题 | 快速解决 |
|------|---------|
| global is not defined | 修改 `vite.config.ts` 添加 `define: { global: 'globalThis' }` |
| 页面空白 | 检查控制台错误，确认后端已启动 |
| WebSocket 连接失败 | 确认后端运行在 8080 端口 |
| CORS 错误 | 检查 `WebConfig.java` 配置 |
| 端口被占用 | 修改端口配置 |
| 编译错误 | 运行 `mvnw clean install` |

---

## 🎯 验证系统正常工作

### 测试场景 1: 单用户登录
1. 访问 http://localhost:5173
2. 输入用户名 "Alice"
3. 点击"加入聊天"
4. ✅ 应该看到聊天界面，状态指示器为绿色

### 测试场景 2: 发送消息
1. 在聊天框输入 "Hello"
2. 点击"发送"
3. ✅ 应该在消息列表中看到自己的消息

### 测试场景 3: 多用户聊天
1. 打开新的浏览器窗口（或隐私模式）
2. 用户名 "Bob" 登录
3. 在任一窗口发送消息
4. ✅ 两个窗口都应该收到消息

### 测试场景 4: 通知功能
1. 在 Alice 窗口，右侧通知面板
2. 接收者填 "Bob"，发送通知
3. ✅ Bob 的窗口应该收到通知

如果所有测试都通过，说明系统运行正常！🎉

---

## 📚 更多帮助

- [快速开始指南](QUICKSTART.md)
- [项目文档](README.md)
- [API 文档](README_WEBSOCKET.md)
