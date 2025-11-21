# 项目架构详解

## 系统架构图

```
┌──────────────────────────────────────────────────────────────────┐
│                        客户端浏览器                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Vue 3 前端应用 (SPA)                        │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │  App.vue                                                │   │
│  │  ├── ChatRoom.vue (群聊界面)                            │   │
│  │  ├── NotificationPanel.vue (通知面板)                  │   │
│  │  ├── FriendPanel.vue (好友列表)                        │   │
│  │  ├── PrivateChatWindow.vue (私聊窗口)                  │   │
│  │  └── GroupManager.vue (群聊管理)                       │   │
│  │                                                        │   │
│  │  Composables:                                         │   │
│  │  └── useWebSocket.ts (WebSocket 管理)                │   │
│  │      ├── connect/disconnect                           │   │
│  │      ├── sendMessage                                   │   │
│  │      ├── sendPrivateMessage                            │   │
│  │      ├── sendGroupMessage                              │   │
│  │      └── 订阅管理                                      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                   ┌─────WebSocket┐                             │
│                   │  (STOMP/SockJS)                            │
│                   └─────────────┘                              │
│                                                                 │
└─────────────────────────────────────────────────────────────┬───┘
                                                              │
                    ┌──────────────────────────────────────┤
                    │
        ┌───────────▼─────────────────────────────┐
        │        Spring Boot 后端服务               │
        ├───────────────────────────────────────┤
        │ http://localhost:8080                 │
        │                                       │
        │  ┌─────────────────────────────────┐  │
        │  │     REST Controllers            │  │
        │  ├─────────────────────────────────┤  │
        │  │  • FriendController             │  │
        │  │    - POST   /api/friends/req    │  │
        │  │    - GET    /api/friends        │  │
        │  │    - DELETE /api/friends/{id}   │  │
        │  │                                 │  │
        │  │  • GroupController              │  │
        │  │    - POST   /api/groups         │  │
        │  │    - GET    /api/groups         │  │
        │  │    - PUT    /api/groups/{id}    │  │
        │  │    - DELETE /api/groups/{id}    │  │
        │  │                                 │  │
        │  │  • NotificationRestController   │  │
        │  │    - POST   /api/notifications │  │
        │  └─────────────────────────────────┘  │
        │                                       │
        │  ┌─────────────────────────────────┐  │
        │  │   WebSocket Controllers          │  │
        │  ├─────────────────────────────────┤  │
        │  │  • ChatController               │  │
        │  │    - @MessageMapping            │  │
        │  │      /app/chat.sendMessage      │  │
        │  │      /app/chat.addUser          │  │
        │  │                                 │  │
        │  │  • GroupChatController          │  │
        │  │    - /app/group.sendMessage/{id}│  │
        │  │    - /app/group.addMember/{id}  │  │
        │  │                                 │  │
        │  │  • NotificationController       │  │
        │  │    - /app/notification.send     │  │
        │  └─────────────────────────────────┘  │
        │                                       │
        │  ┌─────────────────────────────────┐  │
        │  │   Business Services             │  │
        │  ├─────────────────────────────────┤  │
        │  │  • FriendService                │  │
        │  │  • GroupService                 │  │
        │  │  • MessageService               │  │
        │  │  • NotificationService          │  │
        │  └─────────────────────────────────┘  │
        │                                       │
        │  ┌─────────────────────────────────┐  │
        │  │   Configuration                 │  │
        │  ├─────────────────────────────────┤  │
        │  │  • WebSocketConfig              │  │
        │  │    - 启用 STOMP 消息代理        │  │
        │  │    - 配置订阅/发布端点          │  │
        │  │                                 │  │
        │  │  • WebConfig                    │  │
        │  │    - 配置 CORS 跨域             │  │
        │  └─────────────────────────────────┘  │
        │                                       │
        │  ┌─────────────────────────────────┐  │
        │  │   Data Models                   │  │
        │  ├─────────────────────────────────┤  │
        │  │  • ChatMessage                  │  │
        │  │  • Notification                 │  │
        │  │  • Friend                       │  │
        │  │  • FriendRequest                │  │
        │  │  • ChatGroup                    │  │
        │  │  • GroupMessage                 │  │
        │  │  • PrivateMessage               │  │
        │  └─────────────────────────────────┘  │
        │                                       │
        └───────────────────────────────────────┘
```

---

## 数据流

### 1. 群聊消息流

```
用户A
  │
  ├─ 输入消息: "大家好"
  │
  └─► sendMessage() in useWebSocket.ts
      │
      ├─ 构建 ChatMessage 对象
      │   {
      │     type: 'CHAT',
      │     sender: 'Alice',
      │     content: '大家好',
      │     timestamp: 1234567890
      │   }
      │
      └─► stompClient.publish({
            destination: '/app/chat.sendMessage',
            body: JSON.stringify(message)
          })
          
          │
          ▼ WebSocket (STOMP protocol)
          
        Spring Boot Server
        │
        └─► ChatController.sendMessage()
            │
            ├─ 设置时间戳
            │
            └─► @SendTo("/topic/public")
                │
                ▼ 广播给所有订阅了 /topic/public 的客户端
                
            所有已连接的用户
            │
            ├─ 用户A (发送者) ✓
            ├─ 用户B ✓
            ├─ 用户C ✓
            └─ 用户D ✓
            
            每个客户端接收到消息后:
            └─► messages.value.push(chatMessage)
                │
                └─► 更新 Vue 组件，实时显示在聊天界面
```

### 2. 私聊消息流

```
用户A 想给 用户B 发送私聊消息
│
├─ 打开私聊窗口
│
└─► sendPrivateMessage('B', '你好')
    │
    ├─ 构建 PrivateMessage 对象
    │   {
    │     senderId: 'A',
    │     receiverId: 'B',
    │     content: '你好',
    │     timestamp: 1234567890,
    │     isRead: false
    │   }
    │
    └─► stompClient.publish({
          destination: '/app/private.sendMessage/B',
          body: JSON.stringify(message)
        })
        
        │
        ▼ WebSocket (STOMP protocol)
        
      Spring Boot Server
      │
      └─► PrivateChatController.sendMessage()
          │
          ├─ 构建完整消息对象
          │
          └─► stompClient.send to "/user/B/queue/private-chat/A"
              │
              ▼ 只有用户B能接收
              
          用户B
          │
          └─► 订阅了 /user/queue/private-chat/A
              │
              └─► 接收消息并显示在私聊窗口
```

### 3. 群聊消息流

```
用户在群聊 #java-learning 中发送消息
│
├─ selectGroup(groupId: 1)
│
├─ subscribeToGroup(1) ──────┐ (订阅群组频道)
│                             │
├─ sendGroupMessage(1, '内容')│
│  │                          │
│  ├─ 构建 GroupMessage      │
│  │   {                      │
│  │     groupId: 1,          │
│  │     senderId: 'Alice',   │
│  │     content: '...',      │
│  │     timestamp: ...,      │
│  │     status: 'SENT'       │
│  │   }                      │
│  │                          │
│  └─► publish to:            │
│      /app/group.send        │
│      Message/1              │
│                             │
│                   ▼ WebSocket (STOMP)
│                             
│             Spring Boot Server
│             │
│             └─► GroupChatController
│                 .sendGroupMessage()
│                 │
│                 ├─ GroupService
│                 │  .saveGroupMessage()
│                 │
│                 └─► @SendTo("/topic/group/1")
│                     │
│  ┌──────────────────────┼──────────────────────┐
│  │                      │                      │
│  ▼                      ▼                      ▼
│ Alice           Bob (group member)    Charlie (group member)
│ 发送者          接收者                 接收者
│ │               │                     │
│ ├─ 已发送提示   ├─ 收到消息          ├─ 收到消息
│ │               ├─ 显示发送者:Alice  ├─ 显示发送者:Alice
│ │               └─ 添加到消息列表    └─ 添加到消息列表
│ │
│ └─ 更新发送状态为 'DELIVERED'
```

### 4. 好友系统流

```
Alice 要加 Bob 为好友:
│
1. Alice 点击"添加好友"
   │
   └─► sendFriendRequest('Alice', 'Bob', 'Hi Bob!')
       │
       └─► POST /api/friends/request
           {
             "senderId": "Alice",
             "receiverId": "Bob",
             "message": "Hi Bob!"
           }
           │
           ▼ Spring Boot Server
           
         FriendService.sendFriendRequest()
         │
         └─► 存储请求到内存
             {
               id: 1,
               status: 'PENDING'
             }
             
             Response: ✓ 201 Created

2. Bob 登录，查看好友请求
   │
   └─► GET /api/friends/requests?userId=Bob
       │
       ▼ Spring Boot Server
       
     FriendService.getPendingRequests('Bob')
     │
     └─► 返回所有待接受的请求
         [
           {
             id: 1,
             senderId: 'Alice',
             receiverId: 'Bob',
             message: 'Hi Bob!',
             status: 'PENDING'
           }
         ]

3. Bob 接受请求
   │
   └─► PUT /api/friends/requests/1/accept
       │
       ▼ Spring Boot Server
       
     FriendService.acceptFriendRequest(1)
     │
     ├─ 更新请求状态为 'ACCEPTED'
     │
     └─ 创建双向好友关系:
        ├─ Alice → Bob
        └─ Bob → Alice
        
4. 现在双方都能在好友列表中看到对方

5. 私聊通道被激活
   │
   └─► subscribeToPrivateChat('Bob')
       └─► 订阅 /user/queue/private-chat/Bob
           现在可以接收来自 Bob 的私聊消息
```

### 5. 群聊管理流

```
创建群聊:
│
1. Alice 点击"新建群聊"
   │
   └─► createGroup('Java学习', '讨论Java技术', 'Alice')
       │
       └─► POST /api/groups
           {
             "groupName": "Java学习",
             "description": "讨论Java技术",
             "ownerId": "Alice"
           }
           │
           ▼ Spring Boot
           
         GroupService.createGroup()
         │
         └─► 新群聊:
             {
               id: 1,
               groupName: 'Java学习',
               ownerId: 'Alice',
               memberIds: ['Alice'],  // Alice 自动加入
               createdAt: ...
             }

添加成员:
│
2. Alice (群主) 添加 Bob
   │
   └─► addMember(1, 'Bob')
       │
       └─► POST /api/groups/1/members
           {
             "userId": "Bob"
           }
           │
           ▼ Spring Boot
           
         GroupService.addMember(1, 'Bob')
         │
         └─► memberIds: ['Alice', 'Bob']
         
         ✓ 群聊更新

3. Bob 加入群聊
   │
   ├─ subscribeToGroup(1)
   │  └─ 订阅 /topic/group/1
   │
   └─ 现在可以接收和发送群聊消息

删除成员:
│
4. Alice 移除 Charlie
   │
   └─► removeMember(1, 'Charlie')
       │
       └─► DELETE /api/groups/1/members/Charlie
           │
           ▼ Spring Boot
           
         GroupService.removeMember(1, 'Charlie')
         │
         └─► memberIds: ['Alice', 'Bob']
         
         Charlie 被踢出群聊
         （WebSocket 连接会被关闭）

删除群聊:
│
5. Alice (群主) 删除群聊
   │
   └─► deleteGroup(1)
       │
       └─► DELETE /api/groups/1
           │
           ▼ Spring Boot
           
         GroupService.deleteGroup(1)
         │
         ├─ 删除群聊记录
         ├─ 删除所有消息
         └─ 通知所有成员断开连接
```

---

## 技术栈详解

### 前端 (webSocketFront/)

**核心库**:
- Vue 3.5.22 - 渐进式框架
- TypeScript 5.9 - 类型安全
- Vite - 快速构建工具
- SockJS Client - WebSocket 降级方案
- STOMP.js - STOMP 协议支持

**目录结构**:
```
webSocketFront/src/
├── main.ts              # 应用入口
├── App.vue              # 根组件
├── components/          # Vue 组件
│   ├── ChatRoom.vue     # 群聊界面
│   ├── NotificationPanel.vue  # 通知面板
│   ├── FriendPanel.vue  # 好友列表
│   ├── PrivateChatWindow.vue  # 私聊窗口
│   └── GroupManager.vue # 群聊管理
├── composables/         # 组合式函数
│   └── useWebSocket.ts  # WebSocket 管理
├── stores/              # Pinia 状态管理（可选）
├── router/              # Vue Router（可选）
└── types/               # TypeScript 类型定义
    └── index.ts         # 共享类型定义
```

**通信方式**:
- **WebSocket (STOMP)**: 实时双向通信
- **REST API**: 传统 HTTP 请求
- **事件系统**: 组件间通信

### 后端 (src/main/java/)

**核心库**:
- Spring Boot 3.5.7 - 应用框架
- Spring WebSocket - WebSocket 支持
- Spring Messaging - 消息处理
- Lombok - 代码简化
- Jackson - JSON 序列化

**目录结构**:
```
src/main/java/com/muybaby/websocket/
├── WebSocketApplication.java   # 主应用
├── config/                      # 配置类
│   ├── WebSocketConfig.java     # STOMP 配置
│   └── WebConfig.java           # CORS 配置
├── controller/                  # 控制器层
│   ├── ChatController.java      # 群聊
│   ├── GroupChatController.java # 群聊消息
│   ├── FriendController.java    # 好友管理
│   ├── GroupController.java     # 群聊管理
│   ├── NotificationController.java    # WebSocket 通知
│   └── NotificationRestController.java # REST 通知
├── service/                     # 业务层
│   ├── FriendService.java       # 好友服务
│   ├── GroupService.java        # 群聊服务
│   └── MessageService.java      # 消息服务
├── model/                       # 数据模型
│   ├── ChatMessage.java         # 聊天消息
│   ├── Notification.java        # 通知
│   ├── Friend.java              # 好友
│   ├── FriendRequest.java       # 好友请求
│   ├── ChatGroup.java           # 群聊
│   ├── GroupMessage.java        # 群消息
│   └── PrivateMessage.java      # 私聊消息
├── listener/                    # 事件监听
│   └── WebSocketEventListener.java # 连接事件
└── repository/                  # 数据访问层（可选）
    └── 数据库操作接口
```

---

## 消息订阅/发布端点总结

### STOMP 订阅频道 (Subscribe)

| 频道 | 用途 | 接收者 | 说明 |
|------|------|--------|------|
| `/topic/public` | 群聊消息 | 所有连接 | 广播型，所有人都能收到 |
| `/user/queue/notifications` | 个人通知 | 特定用户 | 点对点，只有指定用户能收到 |
| `/topic/notifications` | 广播通知 | 所有连接 | 系统广播，所有人都能收到 |
| `/topic/group/{groupId}` | 群聊消息 | 群成员 | 群内广播，只有群成员能收到 |
| `/user/queue/private-chat/{friendId}` | 私聊消息 | 特定用户 | 私信，只有对方能收到 |

### STOMP 发送端点 (Publish/Subscribe)

| 端点 | 处理器 | 说明 |
|------|--------|------|
| `/app/chat.sendMessage` | ChatController | 发送群聊消息 |
| `/app/chat.addUser` | ChatController | 用户加入通知 |
| `/app/notification.send` | NotificationController | 发送通知 |
| `/app/group.sendMessage/{groupId}` | GroupChatController | 发送群聊消息 |
| `/app/group.addMember/{groupId}` | GroupChatController | 添加群成员 |
| `/app/private.sendMessage/{receiverId}` | PrivateChatController | 发送私聊消息 |

---

## 关键技术特性

### 1. 自动重连机制

```typescript
// useWebSocket.ts 中的配置
const client = new Client({
    webSocketFactory: () => new SockJS(SOCKET_URL),
    reconnectDelay: 5000,  // 5秒后重连
    heartbeatIncoming: 4000,  // 接收心跳
    heartbeatOutgoing: 4000   // 发送心跳
})
```

### 2. CORS 跨域配置

```java
// WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("*")
            .allowedHeaders("*")
    }
}
```

### 3. WebSocket 消息映射

```java
// 使用 @MessageMapping 处理客户端消息
@MessageMapping("/chat.sendMessage")
@SendTo("/topic/public")
public ChatMessage sendMessage(@Payload ChatMessage message) {
    // 处理消息逻辑
    return message;
}

// 支持动态参数
@MessageMapping("/group.sendMessage/{groupId}")
@SendTo("/topic/group/{groupId}")
public GroupMessage sendGroupMessage(@DestinationVariable Long groupId,
                                     @Payload GroupMessage message) {
    // 处理群聊消息
    return message;
}
```

### 4. 用户定向消息

```java
// 使用 @SendToUser 发送私人消息
@MessageMapping("/notification.send")
@SendToUser("/queue/notifications")
public Notification sendNotification(@Payload Notification notification) {
    // 消息只发送给指定用户
    return notification;
}
```

---

## 性能优化建议

1. **消息压缩**: 对大消息进行压缩
2. **消息分页**: 历史消息分页加载
3. **缓存**: Redis 缓存用户信息和群聊数据
4. **异步处理**: 使用 @Async 处理耗时操作
5. **消息队列**: 使用 RabbitMQ/Kafka 处理高并发
6. **数据库**: 使用连接池，优化查询

---

## 安全建议

1. **认证**: Spring Security OAuth2
2. **加密**: 消息端到端加密
3. **限流**: 接口速率限制
4. **验证**: 输入/输出验证
5. **日志**: 审计日志记录
6. **HTTPS/WSS**: 生产环境使用安全协议

