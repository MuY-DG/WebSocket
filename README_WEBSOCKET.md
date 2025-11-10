# WebSocket 实时通信应用

这是一个基于 Spring Boot 和 Vue 3 的 WebSocket 实时通信应用，支持群聊和点对点通知功能。

## 功能特性

✅ **实时群聊** - 多用户可以在聊天室中实时交流
✅ **点对点通知** - 发送私密通知给特定用户
✅ **广播通知** - 向所有在线用户广播通知
✅ **用户状态** - 显示用户加入/离开状态
✅ **消息类型** - 支持信息、成功、警告、错误等多种通知类型
✅ **优雅的UI** - 现代化的渐变色界面设计

## 技术栈

### 后端
- Java 17
- Spring Boot 3.5.7
- Spring WebSocket (STOMP)
- Lombok

### 前端
- Vue 3
- TypeScript
- Vite
- SockJS Client
- STOMP.js

## 项目结构

```
WebSocket/
├── src/main/java/com/muybaby/websocket/
│   ├── config/
│   │   ├── WebSocketConfig.java      # WebSocket 配置
│   │   └── WebConfig.java             # CORS 配置
│   ├── controller/
│   │   ├── ChatController.java        # 群聊控制器
│   │   ├── NotificationController.java      # WebSocket 通知控制器
│   │   └── NotificationRestController.java  # REST 通知控制器
│   ├── model/
│   │   ├── ChatMessage.java           # 聊天消息实体
│   │   └── Notification.java          # 通知实体
│   └── listener/
│       └── WebSocketEventListener.java # WebSocket 事件监听器
│
└── webSocketFront/
    └── src/
        ├── composables/
        │   └── useWebSocket.ts        # WebSocket 组合式函数
        ├── components/
        │   ├── ChatRoom.vue           # 聊天室组件
        │   └── NotificationPanel.vue  # 通知面板组件
        └── App.vue                    # 主应用组件
```

## 快速开始

### 启动后端

1. 进入后端目录并启动：
```bash
cd d:\project-NEW\Java\WebSocket
mvnw spring-boot:run
```

后端将在 `http://localhost:8080` 启动

### 启动前端

1. 安装依赖（如果还未安装）：
```bash
cd webSocketFront
npm install
```

2. 启动开发服务器：
```bash
npm run dev
```

前端将在 `http://localhost:5173` 启动

## 使用说明

### 1. 登录
- 打开浏览器访问前端地址
- 输入用户名并点击"加入聊天"

### 2. 群聊
- 在左侧聊天室中输入消息
- 消息将实时广播给所有在线用户
- 可以看到其他用户的加入/离开状态

### 3. 发送通知
- 在右侧通知面板填写通知信息：
  - 接收者：目标用户的用户名
  - 类型：信息/成功/警告/错误
  - 标题：通知标题
  - 内容：通知详细内容
- 点击"发送通知"按钮

### 4. 接收通知
- 当其他用户发送通知给你时，会在右侧通知面板实时显示
- 不同类型的通知有不同的颜色标识

## API 端点

### WebSocket 端点
- **连接地址**: `ws://localhost:8080/ws`

### STOMP 订阅端点
- `/topic/public` - 群聊消息广播
- `/user/queue/notifications` - 个人通知
- `/topic/notifications` - 广播通知

### STOMP 发送端点
- `/app/chat.sendMessage` - 发送聊天消息
- `/app/chat.addUser` - 用户加入
- `/app/notification.send` - 发送通知

### REST API
- `POST /api/notifications/send` - 通过 HTTP 发送点对点通知
- `POST /api/notifications/broadcast` - 通过 HTTP 广播通知

## 测试建议

1. **多用户测试**：打开多个浏览器窗口（或隐私模式），使用不同的用户名登录
2. **群聊测试**：在一个窗口发送消息，观察其他窗口是否实时收到
3. **通知测试**：在一个窗口向另一个用户发送通知，观察目标窗口是否收到
4. **断线重连**：关闭后端服务，观察前端是否自动尝试重连

## 注意事项

- 确保后端服务在前端之前启动
- 如果端口被占用，请修改配置文件中的端口号
- 通知的接收者必须是已登录的用户名（区分大小写）

## 开发说明

### 修改 WebSocket 地址
如果后端地址有变化，请修改前端文件：
`webSocketFront/src/composables/useWebSocket.ts`
```typescript
const SOCKET_URL = 'http://localhost:8080/ws'  // 修改为实际地址
```

### 修改后端端口
修改 `application.properties`:
```properties
server.port=8080  # 修改为其他端口
```

## 故障排查

### 无法连接 WebSocket
1. 检查后端服务是否正常运行
2. 检查防火墙设置
3. 查看浏览器控制台的错误信息

### 消息无法发送
1. 确认 WebSocket 连接状态（查看状态指示器）
2. 检查浏览器控制台的 STOMP 调试信息
3. 查看后端日志

### 收不到通知
1. 确认接收者用户名是否正确
2. 确认接收者是否已登录
3. 检查是否订阅了正确的通知频道

## License

MIT
