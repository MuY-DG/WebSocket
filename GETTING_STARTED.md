# 快速入门 - 实现好友和群聊功能

## 🎯 概述

本文档提供了一个完整的实现指南，展示如何在这个 WebSocket 实时通信应用中添加：

- ✅ **好友系统** - 添加好友、管理好友请求、删除好友
- ✅ **私聊功能** - 用户间的一对一通信
- ✅ **群聊系统** - 创建、管理、加入/离开群聊
- ✅ **实时消息** - 通过 WebSocket 实现实时同步

---

## 📂 关键文档

在开始之前，请按以下顺序阅读文档：

1. **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** ← **从这里开始**
   - 完整的功能需求分析
   - 数据模型设计
   - 分步实现指南

2. **[ARCHITECTURE.md](ARCHITECTURE.md)**
   - 系统架构图
   - 数据流图
   - 技术栈详解

3. **[CODE_EXAMPLES.md](CODE_EXAMPLES.md)**
   - 后端完整代码示例
   - 前端 Vue 3 组件示例
   - 最佳实践和常见问题

4. **[QUICKSTART.md](QUICKSTART.md)** (原文档)
   - 项目启动方式
   - 基础功能使用

---

## 🚀 快速开始

### 第一步：理解项目现状

项目已有功能：
```
✅ 群聊（公共频道）
✅ 点对点通知
✅ 用户加入/离开提醒
```

项目结构：
```
/home/engine/project/
├── src/main/java/com/muybaby/websocket/
│   ├── config/        # WebSocket STOMP 配置
│   ├── controller/    # HTTP 和 WebSocket 控制器
│   ├── model/         # 数据模型
│   └── listener/      # 事件监听器
│
└── webSocketFront/src/
    ├── composables/   # WebSocket 管理 (useWebSocket.ts)
    ├── components/    # Vue 组件
    ├── types/         # TypeScript 类型定义（新增）
    └── App.vue        # 主应用
```

### 第二步：实现好友系统

按照 [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) 的后端步骤：

1. **创建数据模型** (10分钟)
   ```
   Friend.java          - 好友关系
   FriendRequest.java   - 好友请求
   ```

2. **创建业务服务** (15分钟)
   ```
   FriendService.java   - 好友业务逻辑
   ```

3. **创建 REST 控制器** (10分钟)
   ```
   FriendController.java - HTTP API 端点
   ```

### 第三步：实现群聊系统

1. **创建数据模型** (10分钟)
   ```
   ChatGroup.java       - 群聊
   GroupMessage.java    - 群消息
   ```

2. **创建业务服务** (15分钟)
   ```
   GroupService.java    - 群聊业务逻辑
   ```

3. **创建 WebSocket 控制器** (10分钟)
   ```
   GroupChatController.java - WebSocket 消息处理
   ```

4. **创建 REST 控制器** (10分钟)
   ```
   GroupController.java - HTTP API 端点
   ```

### 第四步：实现前端界面

1. **扩展 WebSocket composable** (20分钟)
   ```
   useWebSocket.ts - 添加私聊和群聊功能
   ```

2. **创建前端组件** (30分钟)
   ```
   FriendPanel.vue              - 好友列表
   PrivateChatWindow.vue        - 私聊窗口
   GroupManager.vue             - 群聊管理
   GroupChatWindow.vue          - 群聊窗口
   ```

3. **在主应用中集成** (10分钟)
   ```
   App.vue - 整合所有组件
   ```

---

## 💡 核心实现原理

### 好友系统流程

```
Step 1: Alice 发送好友请求给 Bob
        ↓
POST /api/friends/request
{
  senderId: "Alice",
  receiverId: "Bob",
  message: "Hi Bob!"
}
        ↓
Step 2: 请求保存到数据库
        ↓
Step 3: Bob 查看待接受的请求
        ↓
GET /api/friends/requests?userId=Bob
        ↓
Step 4: Bob 接受请求
        ↓
PUT /api/friends/requests/1/accept
        ↓
Step 5: 创建双向好友关系
        ↓
Step 6: 现在双方都能看到对方
        └─► 可以开始私聊了！
```

### 群聊系统流程

```
Step 1: Alice 创建群聊 "Java Learning"
        ↓
POST /api/groups
{
  groupName: "Java Learning",
  description: "Discuss Java technology",
  ownerId: "Alice"
}
        ↓
Step 2: 群聊创建，Alice 自动加入
        ↓
Step 3: Alice 邀请 Bob 加入
        ↓
POST /api/groups/1/members
{
  userId: "Bob"
}
        ↓
Step 4: Bob 订阅群聊频道
        ↓
subscribeToGroup(1)
        ↓
Step 5: Alice 发送消息
        ↓
sendGroupMessage(1, "Hello everyone!")
        ↓
        通过 WebSocket 广播到所有群成员
        ↓
Step 6: Bob 和所有成员实时接收消息
```

### 私聊系统流程

```
Step 1: Alice 要给 Bob 发私聊
        ↓
Step 2: Alice 订阅 Bob 的私聊频道
        ↓
subscribeToPrivateChat("Bob")
        ↓
Step 3: Alice 发送私聊消息
        ↓
sendPrivateMessage("Bob", "Hi Bob!")
        ↓
        通过 WebSocket 发送到 /app/private.sendMessage/Bob
        ↓
Step 4: 后端路由到 Bob 的专用消息队列
        ↓
/user/Bob/queue/private-chat/Alice
        ↓
Step 5: 只有 Bob 能收到这条消息
```

---

## 🔌 WebSocket 端点速查表

### 订阅（接收消息）

| 端点 | 用途 | 谁能收到 |
|------|------|--------|
| `/topic/public` | 群聊 | 所有人 |
| `/topic/group/{groupId}` | 群聊消息 | 群成员 |
| `/user/queue/private-chat/{friendId}` | 私聊 | 特定用户 |
| `/user/queue/notifications` | 通知 | 特定用户 |

### 发布（发送消息）

| 端点 | 用途 | 说明 |
|------|------|------|
| `/app/chat.sendMessage` | 群聊消息 | 广播到 /topic/public |
| `/app/group.sendMessage/{groupId}` | 群聊消息 | 广播到 /topic/group/{id} |
| `/app/private.sendMessage/{userId}` | 私聊 | 发送到用户的队列 |
| `/app/notification.send` | 通知 | 发送给特定用户 |

---

## 📊 数据库模式

### 好友关系表

```sql
CREATE TABLE friends (
  id BIGINT PRIMARY KEY,
  user_id VARCHAR(100),
  friend_id VARCHAR(100),
  status VARCHAR(20),  -- PENDING, ACCEPTED, REJECTED
  created_at BIGINT,
  updated_at BIGINT
);

CREATE TABLE friend_requests (
  id BIGINT PRIMARY KEY,
  sender_id VARCHAR(100),
  receiver_id VARCHAR(100),
  message TEXT,
  status VARCHAR(20),
  created_at BIGINT,
  updated_at BIGINT
);
```

### 群聊表

```sql
CREATE TABLE chat_groups (
  id BIGINT PRIMARY KEY,
  group_name VARCHAR(100),
  description TEXT,
  owner_id VARCHAR(100),
  created_at BIGINT,
  updated_at BIGINT
);

CREATE TABLE group_members (
  id BIGINT PRIMARY KEY,
  group_id BIGINT,
  user_id VARCHAR(100),
  role VARCHAR(20),  -- admin, member
  joined_at BIGINT,
  status VARCHAR(20)
);

CREATE TABLE group_messages (
  id BIGINT PRIMARY KEY,
  group_id BIGINT,
  sender_id VARCHAR(100),
  content TEXT,
  timestamp BIGINT,
  status VARCHAR(20)
);
```

### 私聊表

```sql
CREATE TABLE private_messages (
  id BIGINT PRIMARY KEY,
  sender_id VARCHAR(100),
  receiver_id VARCHAR(100),
  content TEXT,
  timestamp BIGINT,
  is_read BOOLEAN
);
```

---

## 🧪 测试清单

### 好友系统测试

- [ ] 发送好友请求
- [ ] 查看待接受的请求
- [ ] 接受好友请求
- [ ] 拒绝好友请求
- [ ] 查看好友列表
- [ ] 删除好友

### 私聊系统测试

- [ ] 订阅私聊频道
- [ ] 发送私聊消息
- [ ] 接收私聊消息
- [ ] 显示聊天记录
- [ ] 清除聊天记录

### 群聊系统测试

- [ ] 创建群聊
- [ ] 加入群聊
- [ ] 退出群聊
- [ ] 发送群聊消息
- [ ] 接收群聊消息
- [ ] 添加群成员
- [ ] 移除群成员
- [ ] 删除群聊

---

## 🛠️ 常用命令

### 启动应用

```bash
# 方式1：一键启动（推荐）
cd /home/engine/project
./mvnw spring-boot:run  # 后端
# 新终端
cd webSocketFront
npm run dev  # 前端

# 方式2：构建并运行
mvn clean package
java -jar target/websocket-app.jar
```

### 测试 API

```bash
# 发送好友请求
curl -X POST http://localhost:8080/api/friends/request \
  -H "Content-Type: application/json" \
  -d '{"senderId":"Alice","receiverId":"Bob","message":"Hi!"}'

# 获取好友列表
curl http://localhost:8080/api/friends?userId=Alice

# 创建群聊
curl -X POST http://localhost:8080/api/groups \
  -H "Content-Type: application/json" \
  -d '{"groupName":"Java","description":"Learn Java","ownerId":"Alice"}'

# 获取群聊列表
curl http://localhost:8080/api/groups
```

---

## 📈 实现时间估计

| 任务 | 时间 | 难度 |
|------|------|------|
| 好友系统后端 | 35 分钟 | ⭐⭐ |
| 好友系统前端 | 20 分钟 | ⭐⭐ |
| 群聊系统后端 | 45 分钟 | ⭐⭐⭐ |
| 群聊系统前端 | 30 分钟 | ⭐⭐⭐ |
| 私聊系统后端 | 20 分钟 | ⭐⭐ |
| 私聊系统前端 | 30 分钟 | ⭐⭐⭐ |
| **总计** | **~3 小时** | |

---

## 🔒 下一步改进

生产环境部署清单：

- [ ] 集成真实数据库（MySQL/PostgreSQL）
- [ ] 添加 Spring Security 认证
- [ ] 配置 HTTPS/WSS
- [ ] 添加消息持久化
- [ ] 实现消息加密
- [ ] 添加日志审计
- [ ] 性能优化（Redis 缓存）
- [ ] 单元测试覆盖
- [ ] 集成测试

---

## 📚 资源链接

- [Spring WebSocket 文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [STOMP 协议规范](https://stomp.github.io/)
- [Vue 3 官方文档](https://vuejs.org/)
- [TypeScript 文档](https://www.typescriptlang.org/docs/)

---

## ❓ 获取帮助

### 遇到问题？

1. 查看 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. 查看 [CODE_EXAMPLES.md](CODE_EXAMPLES.md) 的常见问题部分
3. 检查浏览器控制台错误
4. 查看后端日志

### 有建议或问题？

- 检查现有的相关代码
- 参考项目文件清单：[PROJECT_FILES.md](PROJECT_FILES.md)
- 查看通知系统文档：[NOTIFICATION_UPDATE.md](NOTIFICATION_UPDATE.md)

---

## 📝 笔记

这个实现指南基于该项目的现有架构：
- ✅ WebSocket + STOMP 已配置
- ✅ CORS 已启用
- ✅ 基础的消息处理已完成
- ✅ 前端 Vue 3 基础已搭建

你需要做的是在这个基础上扩展功能。

---

<div align="center">

**开始实现吧！** 🚀

</div>

