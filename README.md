# WebSocket 实时通信应用

<div align="center">

![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-green)
![Vue](https://img.shields.io/badge/Vue-3.5.22-brightgreen)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue)

一个功能完整的实时通信应用，支持群聊和点对点通知

[快速开始](#-快速开始) • [功能特性](#-主要特性) • [技术栈](#️-技术栈) • [文档](#-文档)

</div>

---

## 📖 简介

这是一个基于 **Spring Boot** 和 **Vue 3** 构建的 WebSocket 实时通信应用。使用 STOMP 协议实现了群聊、点对点通知和系统广播功能，具有现代化的用户界面和完善的错误处理机制。

### ✨ 主要特性

- 🗨️ **实时群聊** - 多用户同时在线聊天，消息实时同步
- 🔔 **点对点通知** - 向特定用户发送私密通知
- 📢 **系统广播** - 向所有在线用户发送公告
- 👥 **用户状态** - 实时显示用户加入/离开状态
- 🎨 **现代UI** - 优雅的渐变色设计，响应式布局
- 🔌 **双协议支持** - WebSocket + REST API

---

## 🚀 快速开始

### 前置要求

- Java 17+
- Node.js 20.19.0+
- Maven 3.6+

### 一键启动

```powershell
# 在项目根目录执行
.\start.ps1
```

### 手动启动

**启动后端：**
```bash
.\mvnw.cmd spring-boot:run
```

**启动前端：**
```bash
cd webSocketFront
npm install
npm run dev
```

### 访问应用

| 服务 | 地址 |
|------|------|
| 前端应用 | http://localhost:5173 |
| 后端API | http://localhost:8080 |
| 测试工具 | http://localhost:8080/test.html |

---

## 💡 使用示例

### 1. 群聊演示

```typescript
// 连接 WebSocket
connect('Alice')

// 发送消息
sendMessage('大家好！')

// 接收消息
messages: [
  { type: 'CHAT', sender: 'Bob', content: '你好 Alice!' }
]
```

### 2. 发送通知

**通过 WebSocket：**
```typescript
sendNotification({
  recipient: 'Bob',
  type: 'INFO',
  title: '新消息',
  message: '你有一条新通知'
})
```

**通过 REST API：**
```bash
curl -X POST http://localhost:8080/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "Bob",
    "type": "SUCCESS",
    "title": "操作成功",
    "message": "您的操作已完成"
  }'
```

---

## 🏗️ 技术栈

### 后端
- **Spring Boot 3.5.7** - 应用框架
- **Spring WebSocket** - WebSocket 支持
- **STOMP** - 消息协议
- **Lombok** - 简化代码

### 前端
- **Vue 3** - 渐进式框架
- **TypeScript** - 类型安全
- **Vite** - 构建工具
- **SockJS** - WebSocket 降级
- **STOMP.js** - STOMP 客户端

---

## 📁 项目结构

```
WebSocket/
├── src/main/java/com/muybaby/websocket/
│   ├── config/              # 配置类
│   ├── controller/          # 控制器
│   ├── model/               # 实体类
│   └── listener/            # 事件监听器
│
├── webSocketFront/
│   └── src/
│       ├── composables/     # 组合式函数
│       ├── components/      # Vue 组件
│       └── App.vue          # 主应用
│
├── start.ps1                # 启动脚本
└── QUICKSTART.md            # 详细文档
```

---

## 📚 文档

- **[快速开始指南](QUICKSTART.md)** - 详细的使用教程
- **[项目说明](README_WEBSOCKET.md)** - 功能和API文档
- **[文件清单](PROJECT_FILES.md)** - 所有文件列表

---

## 🔌 WebSocket 端点

### 连接端点
```
ws://localhost:8080/ws
```

### 订阅频道
- `/topic/public` - 群聊消息
- `/user/queue/notifications` - 个人通知
- `/topic/notifications` - 系统广播

### 发送端点
- `/app/chat.sendMessage` - 发送消息
- `/app/chat.addUser` - 加入聊天
- `/app/notification.send` - 发送通知

---

## 🧪 测试

### 多用户测试
1. 打开多个浏览器窗口
2. 使用不同用户名登录
3. 测试消息和通知功能

### API 测试
访问 `http://localhost:8080/test.html` 使用可视化测试工具

---

## 🎯 核心功能代码

### 后端 - 群聊控制器
```java
@MessageMapping("/chat.sendMessage")
@SendTo("/topic/public")
public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
    chatMessage.setTimestamp(System.currentTimeMillis());
    return chatMessage;
}
```

### 前端 - WebSocket 连接
```typescript
const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  onConnect: () => {
    client.subscribe('/topic/public', (message) => {
      // 处理消息
    })
  }
})
```

---

## 🛠️ 配置说明

### 修改后端端口
编辑 `application.properties`:
```properties
server.port=8080
```

### 修改 WebSocket 地址
编辑 `useWebSocket.ts`:
```typescript
const SOCKET_URL = 'http://localhost:8080/ws'
```

---

## 🐛 故障排查

**无法连接 WebSocket？**
- 检查后端服务是否运行
- 查看浏览器控制台错误
- 确认防火墙设置

**收不到通知？**
- 确认接收者用户名正确
- 确认接收者已登录
- 检查 WebSocket 连接状态

---

## 🎨 界面预览

- 🎨 渐变色背景
- 💬 实时消息气泡
- 🔔 多彩通知卡片
- 🟢 连接状态指示器
- 📱 响应式设计

---

## 📊 性能指标

- ⚡ 消息延迟 < 100ms
- 🔄 自动重连机制
- 💾 轻量级客户端
- 🚀 高并发支持

---

## 🔐 安全建议

生产环境部署时建议：
- [ ] 添加用户认证（Spring Security）
- [ ] 配置 HTTPS/WSS
- [ ] 限制 CORS 域名
- [ ] 添加消息速率限制
- [ ] 启用消息加密

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📄 许可证

MIT License

---

## 🎓 学习资源

- [Spring WebSocket 文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP 协议](https://stomp.github.io/)
- [Vue 3 文档](https://vuejs.org/)

---

<div align="center">

**如有问题，请查看 [QUICKSTART.md](QUICKSTART.md) 获取详细帮助**

Made with ❤️ using Spring Boot & Vue 3

</div>
