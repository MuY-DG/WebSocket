# 🚀 WebSocket 实时通信应用 - 快速开始指南

## 📋 项目概述

这是一个功能完整的 WebSocket 实时通信应用，支持：
- ✅ **群聊功能** - 多用户实时聊天
- ✅ **点对点通知** - 向特定用户发送通知
- ✅ **广播通知** - 向所有用户发送通知
- ✅ **用户状态** - 加入/离开提醒
- ✅ **多种通知类型** - 信息/成功/警告/错误

---

## 🎯 快速启动（三种方式）

### 方式 1️⃣：一键启动（推荐）

```powershell
# 在项目根目录执行
.\start.ps1
```

这将自动启动后端和前端服务。

### 方式 2️⃣：分别启动

**启动后端：**
```powershell
.\start-backend.ps1
```

**启动前端：**
```powershell
.\start-frontend.ps1
```

### 方式 3️⃣：手动启动

**后端：**
```powershell
cd d:\project-NEW\Java\WebSocket
.\mvnw.cmd spring-boot:run
```

**前端：**
```powershell
cd d:\project-NEW\Java\WebSocket\webSocketFront
npm run dev
```

---

## 🌐 访问地址

启动成功后，可以访问以下地址：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端应用 | http://localhost:5173 | Vue 3 聊天应用 |
| 后端API | http://localhost:8080 | Spring Boot 服务 |
| 欢迎页面 | http://localhost:8080 | 项目介绍页 |
| API测试 | http://localhost:8080/test.html | REST API 测试工具 |

---

## 📱 使用教程

### 1. 登录系统

1. 打开浏览器访问 `http://localhost:5173`
2. 输入用户名（例如：Alice）
3. 点击"加入聊天"按钮

### 2. 测试群聊

1. 打开**第二个浏览器窗口**（或隐私模式）
2. 访问同样的地址，用不同用户名登录（例如：Bob）
3. 在任一窗口发送消息
4. 观察两个窗口是否都能实时收到消息

### 3. 测试通知

**发送点对点通知：**
1. 在 Alice 的窗口，在右侧通知面板填写：
   - 接收者：`Bob`
   - 类型：选择任意类型
   - 标题：`你好`
   - 内容：`这是给你的通知`
2. 点击"发送通知"
3. 在 Bob 的窗口查看是否收到通知

**使用 REST API 发送通知：**
1. 访问 `http://localhost:8080/test.html`
2. 填写通知信息
3. 点击"发送通知"或"发送广播"

---

## 🔧 技术架构

### 后端技术栈
```
Java 17
├── Spring Boot 3.5.7
├── Spring WebSocket
├── STOMP 协议
└── Lombok
```

### 前端技术栈
```
Vue 3 + TypeScript
├── Vite
├── SockJS Client
├── STOMP.js
└── Composition API
```

---

## 📂 项目结构

```
WebSocket/
│
├── 📁 src/main/java/com/muybaby/websocket/
│   ├── 📁 config/
│   │   ├── WebSocketConfig.java      # WebSocket STOMP 配置
│   │   └── WebConfig.java             # CORS 跨域配置
│   │
│   ├── 📁 controller/
│   │   ├── ChatController.java        # 群聊控制器
│   │   ├── NotificationController.java        # WebSocket 通知
│   │   └── NotificationRestController.java    # HTTP REST 通知
│   │
│   ├── 📁 model/
│   │   ├── ChatMessage.java           # 聊天消息模型
│   │   └── Notification.java          # 通知消息模型
│   │
│   └── 📁 listener/
│       └── WebSocketEventListener.java # 连接事件监听
│
├── 📁 webSocketFront/src/
│   ├── composables/
│   │   └── useWebSocket.ts            # WebSocket 组合函数
│   │
│   ├── components/
│   │   ├── ChatRoom.vue               # 聊天室组件
│   │   └── NotificationPanel.vue      # 通知面板组件
│   │
│   └── App.vue                        # 主应用
│
├── start.ps1                          # 一键启动脚本
├── start-backend.ps1                  # 启动后端脚本
└── start-frontend.ps1                 # 启动前端脚本
```

---

## 🔌 WebSocket 端点

### STOMP 连接端点
```
ws://localhost:8080/ws
```

### 订阅频道
| 频道 | 用途 | 说明 |
|------|------|------|
| `/topic/public` | 群聊 | 所有用户接收群聊消息 |
| `/user/queue/notifications` | 私有通知 | 接收发给自己的通知 |
| `/topic/notifications` | 广播通知 | 所有用户接收系统通知 |

### 发送端点
| 端点 | 用途 |
|------|------|
| `/app/chat.sendMessage` | 发送聊天消息 |
| `/app/chat.addUser` | 用户加入通知 |
| `/app/notification.send` | 发送私有通知 |

---

## 🌐 REST API

### 发送点对点通知
```http
POST http://localhost:8080/api/notifications/send
Content-Type: application/json

{
  "recipient": "Bob",
  "type": "INFO",
  "title": "测试通知",
  "message": "这是通知内容"
}
```

### 发送广播通知
```http
POST http://localhost:8080/api/notifications/broadcast
Content-Type: application/json

{
  "recipient": "all",
  "type": "WARNING",
  "title": "系统维护",
  "message": "系统将在10分钟后维护"
}
```

### 通知类型
- `INFO` - 信息（蓝色）
- `SUCCESS` - 成功（绿色）
- `WARNING` - 警告（橙色）
- `ERROR` - 错误（红色）

---

## 🧪 测试场景

### 场景 1：多人聊天
1. 打开 3 个浏览器窗口
2. 分别用 Alice、Bob、Charlie 登录
3. 在任意窗口发送消息
4. ✅ 所有窗口都能收到消息

### 场景 2：定向通知
1. Alice 在通知面板输入接收者 `Bob`
2. 填写通知内容并发送
3. ✅ 只有 Bob 收到通知，Charlie 不会收到

### 场景 3：系统广播
1. 打开测试工具 `http://localhost:8080/test.html`
2. 使用"广播通知"功能
3. ✅ 所有在线用户都收到通知

### 场景 4：用户进出
1. Alice 已在聊天室
2. Bob 登录
3. ✅ Alice 看到 "Bob 加入了聊天室"
4. Bob 关闭浏览器
5. ✅ Alice 看到 "Bob 离开了聊天室"

---

## ⚠️ 常见问题

### Q: 前端无法连接 WebSocket？
**A:** 
1. 检查后端是否已启动
2. 访问 `http://localhost:8080` 确认后端正常
3. 查看浏览器控制台错误信息

### Q: 发送通知但收不到？
**A:**
1. 确认接收者用户名拼写正确（区分大小写）
2. 确认接收者已登录
3. 打开浏览器开发者工具查看 WebSocket 连接状态

### Q: 端口被占用？
**A:** 修改配置文件：
- 后端：`src/main/resources/application.properties` 修改 `server.port`
- 前端：`webSocketFront/vite.config.ts` 修改端口

### Q: 编译错误？
**A:**
```powershell
# 清理并重新编译
cd d:\project-NEW\Java\WebSocket
.\mvnw.cmd clean install
```

---

## 🎨 自定义配置

### 修改后端端口
编辑 `src/main/resources/application.properties`:
```properties
server.port=8080  # 改为其他端口
```

### 修改 WebSocket 地址
编辑 `webSocketFront/src/composables/useWebSocket.ts`:
```typescript
const SOCKET_URL = 'http://localhost:8080/ws'  // 修改地址
```

---

## 📊 性能优化建议

1. **生产环境**：使用 Redis 作为消息代理
2. **负载均衡**：配置 Nginx 反向代理
3. **安全性**：添加 Spring Security 认证
4. **消息持久化**：集成数据库存储历史消息

---

## 🔐 生产部署清单

- [ ] 修改 CORS 配置，限制允许的域名
- [ ] 添加用户认证和授权
- [ ] 配置 HTTPS/WSS
- [ ] 设置消息速率限制
- [ ] 添加日志监控
- [ ] 配置数据库持久化
- [ ] 设置会话超时时间

---

## 📝 开发笔记

### 添加新的通知类型
1. 在 `Notification.java` 的 `NotificationType` 枚举中添加
2. 在前端 `NotificationPanel.vue` 的 select 中添加选项
3. 更新 `getNotificationIcon` 和颜色样式

### 添加新的消息类型
1. 在 `ChatMessage.java` 的 `MessageType` 枚举中添加
2. 在 `ChatController.java` 添加处理方法
3. 在前端 `ChatRoom.vue` 添加消息渲染逻辑

---

## 📚 学习资源

- [Spring WebSocket 官方文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP 协议规范](https://stomp.github.io/)
- [Vue 3 文档](https://vuejs.org/)
- [SockJS 文档](https://github.com/sockjs/sockjs-client)

---

## 📧 技术支持

如有问题，请检查：
1. 浏览器控制台（F12）
2. 后端日志输出
3. 网络请求（Network 标签）

---

**🎉 祝你使用愉快！**
