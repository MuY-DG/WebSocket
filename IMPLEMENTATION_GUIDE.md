# 好友沟通和群聊 CRUD 实现指南

## 📚 目录

1. [项目架构概述](#项目架构概述)
2. [实现方案设计](#实现方案设计)
3. [后端实现步骤](#后端实现步骤)
4. [前端实现步骤](#前端实现步骤)
5. [完整示例代码](#完整示例代码)
6. [测试指南](#测试指南)

---

## 项目架构概述

### 当前系统设计

```
┌─────────────────────────────────────────────────────────────┐
│                     WebSocket 实时通信架构                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  前端应用 (Vue 3)              WebSocket Server (Spring Boot)│
│  ┌──────────────────┐          ┌──────────────────┐         │
│  │ ChatRoom.vue     │◄───────►│ ChatController   │         │
│  │                  │          │ (处理群聊消息)   │         │
│  ├──────────────────┤          ├──────────────────┤         │
│  │ NotificationPanel│◄───────►│ Notification     │         │
│  │ .vue             │          │ Controller       │         │
│  │                  │          │ (处理通知)       │         │
│  └──────────────────┘          └──────────────────┘         │
│         │                               │                   │
│         └─────SockJS/STOMP Connection──┘                   │
│              (自动重连)                                      │
│                                                              │
│  订阅频道 (Subscribe):                                       │
│  • /topic/public              - 群聊消息                     │
│  • /user/queue/notifications  - 个人通知                     │
│  • /topic/notifications       - 广播通知                     │
│                                                              │
│  发送端点 (Publish):                                         │
│  • /app/chat.sendMessage      - 发送聊天消息                │
│  • /app/notification.send     - 发送通知                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 实现方案设计

### 核心功能需求分析

#### 1. **好友系统** (Friend Management)

**数据模型**:
```
Friend:
├── id (主键)
├── userId (用户ID)
├── friendId (好友ID)
├── friendName (好友昵称)
├── status (状态: 待接受/已接受/已拒绝)
├── createdAt (创建时间)
└── updatedAt (更新时间)

FriendRequest:
├── id (主键)
├── senderId (发送者)
├── receiverId (接收者)
├── message (请求消息)
├── status (状态: 待接受/已接受/已拒绝)
├── createdAt (创建时间)
└── updatedAt (更新时间)
```

**API 端点**:
```
POST   /api/friends/request          - 发送好友请求
GET    /api/friends/requests         - 获取待接受的好友请求
PUT    /api/friends/requests/{id}    - 接受/拒绝好友请求
DELETE /api/friends/{friendId}       - 删除好友
GET    /api/friends                  - 获取好友列表
GET    /api/friends/online           - 获取在线好友
```

#### 2. **私聊系统** (Private Chat)

**WebSocket 端点**:
```
/app/private.sendMessage      - 发送私聊消息
/user/queue/private-chat/{friendId} - 接收私聊消息
```

**数据模型**:
```
PrivateMessage:
├── id
├── senderId
├── receiverId
├── content
├── timestamp
└── isRead (已读状态)
```

#### 3. **群聊系统** (Group Chat)

**数据模型**:
```
ChatGroup:
├── id (主键)
├── groupName (群名称)
├── description (群描述)
├── ownerId (群主ID)
├── avatarUrl (群头像)
├── members (成员列表)
├── createdAt
└── updatedAt

GroupMember:
├── id
├── groupId
├── userId
├── role (admin/member)
├── joinedAt
└── status (active/inactive)

GroupMessage:
├── id
├── groupId
├── senderId
├── content
├── timestamp
└── attachments
```

**API 端点**:
```
POST   /api/groups              - 创建群聊
GET    /api/groups              - 获取群聊列表
GET    /api/groups/{id}         - 获取群聊详情
PUT    /api/groups/{id}         - 更新群聊
DELETE /api/groups/{id}         - 删除群聊
POST   /api/groups/{id}/members - 添加成员
DELETE /api/groups/{id}/members/{userId} - 移除成员
GET    /api/groups/{id}/messages - 获取群聊消息
```

**WebSocket 端点**:
```
/app/group.sendMessage        - 发送群聊消息
/topic/group/{groupId}        - 接收群聊消息
/app/group.addMember          - 添加成员
/app/group.removeMember       - 移除成员
```

---

## 后端实现步骤

### 步骤 1: 创建数据库实体类

#### 1.1 好友相关实体

**src/main/java/com/muybaby/websocket/model/Friend.java**

```java
package com.muybaby.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Friend {
    private Long id;
    private String userId;
    private String friendId;
    private String friendName;
    private FriendStatus status;
    private Long createdAt;
    private Long updatedAt;
    
    public enum FriendStatus {
        PENDING,      // 待接受
        ACCEPTED,     // 已接受
        REJECTED      // 已拒绝
    }
}
```

**src/main/java/com/muybaby/websocket/model/FriendRequest.java**

```java
package com.muybaby.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {
    private Long id;
    private String senderId;
    private String receiverId;
    private String message;
    private RequestStatus status;
    private Long createdAt;
    private Long updatedAt;
    
    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}
```

#### 1.2 群聊相关实体

**src/main/java/com/muybaby/websocket/model/ChatGroup.java**

```java
package com.muybaby.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroup {
    private Long id;
    private String groupName;
    private String description;
    private String ownerId;
    private String avatarUrl;
    private List<String> memberIds;
    private Long createdAt;
    private Long updatedAt;
}
```

**src/main/java/com/muybaby/websocket/model/GroupMessage.java**

```java
package com.muybaby.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessage {
    private Long id;
    private Long groupId;
    private String senderId;
    private String content;
    private Long timestamp;
    private MessageStatus status;
    
    public enum MessageStatus {
        SENT,
        DELIVERED,
        READ
    }
}
```

**src/main/java/com/muybaby/websocket/model/PrivateMessage.java**

```java
package com.muybaby.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessage {
    private Long id;
    private String senderId;
    private String receiverId;
    private String content;
    private Long timestamp;
    private Boolean isRead;
}
```

### 步骤 2: 创建业务服务层

#### 2.1 好友服务

**src/main/java/com/muybaby/websocket/service/FriendService.java**

```java
package com.muybaby.websocket.service;

import com.muybaby.websocket.model.Friend;
import com.muybaby.websocket.model.FriendRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FriendService {
    
    // 模拟数据库存储（生产环境应使用真实数据库）
    private final ConcurrentHashMap<String, List<Friend>> friendsDB = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, FriendRequest> requestsDB = new ConcurrentHashMap<>();
    private Long requestIdGenerator = 1L;
    
    /**
     * 发送好友请求
     */
    public FriendRequest sendFriendRequest(String senderId, String receiverId, String message) {
        FriendRequest request = new FriendRequest();
        request.setId(requestIdGenerator++);
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setMessage(message);
        request.setStatus(FriendRequest.RequestStatus.PENDING);
        request.setCreatedAt(System.currentTimeMillis());
        request.setUpdatedAt(System.currentTimeMillis());
        
        requestsDB.put(request.getId(), request);
        return request;
    }
    
    /**
     * 获取待接受的好友请求
     */
    public List<FriendRequest> getPendingRequests(String userId) {
        return requestsDB.values().stream()
                .filter(r -> r.getReceiverId().equals(userId) && 
                           r.getStatus() == FriendRequest.RequestStatus.PENDING)
                .toList();
    }
    
    /**
     * 接受好友请求
     */
    public void acceptFriendRequest(Long requestId) {
        FriendRequest request = requestsDB.get(requestId);
        if (request != null) {
            request.setStatus(FriendRequest.RequestStatus.ACCEPTED);
            request.setUpdatedAt(System.currentTimeMillis());
            
            // 创建双向好友关系
            addFriend(request.getSenderId(), request.getReceiverId());
            addFriend(request.getReceiverId(), request.getSenderId());
        }
    }
    
    /**
     * 拒绝好友请求
     */
    public void rejectFriendRequest(Long requestId) {
        FriendRequest request = requestsDB.get(requestId);
        if (request != null) {
            request.setStatus(FriendRequest.RequestStatus.REJECTED);
            request.setUpdatedAt(System.currentTimeMillis());
        }
    }
    
    /**
     * 添加好友
     */
    private void addFriend(String userId, String friendId) {
        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(Friend.FriendStatus.ACCEPTED);
        friend.setCreatedAt(System.currentTimeMillis());
        friend.setUpdatedAt(System.currentTimeMillis());
        
        friendsDB.computeIfAbsent(userId, k -> new java.util.ArrayList<>()).add(friend);
    }
    
    /**
     * 获取好友列表
     */
    public List<Friend> getFriends(String userId) {
        return friendsDB.getOrDefault(userId, new java.util.ArrayList<>());
    }
    
    /**
     * 删除好友
     */
    public void removeFriend(String userId, String friendId) {
        List<Friend> friends = friendsDB.get(userId);
        if (friends != null) {
            friends.removeIf(f -> f.getFriendId().equals(friendId));
        }
        
        // 删除双向关系
        List<Friend> friendsFriend = friendsDB.get(friendId);
        if (friendsFriend != null) {
            friendsFriend.removeIf(f -> f.getFriendId().equals(userId));
        }
    }
}
```

#### 2.2 群聊服务

**src/main/java/com/muybaby/websocket/service/GroupService.java**

```java
package com.muybaby.websocket.service;

import com.muybaby.websocket.model.ChatGroup;
import com.muybaby.websocket.model.GroupMessage;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupService {
    
    private final ConcurrentHashMap<Long, ChatGroup> groupsDB = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<GroupMessage>> messagesDB = new ConcurrentHashMap<>();
    private Long groupIdGenerator = 1L;
    private Long messageIdGenerator = 1L;
    
    /**
     * 创建群聊
     */
    public ChatGroup createGroup(String groupName, String description, String ownerId) {
        ChatGroup group = new ChatGroup();
        group.setId(groupIdGenerator++);
        group.setGroupName(groupName);
        group.setDescription(description);
        group.setOwnerId(ownerId);
        group.setMemberIds(new ArrayList<>(List.of(ownerId))); // 群主自动加入
        group.setCreatedAt(System.currentTimeMillis());
        group.setUpdatedAt(System.currentTimeMillis());
        
        groupsDB.put(group.getId(), group);
        return group;
    }
    
    /**
     * 获取群聊列表
     */
    public List<ChatGroup> getGroups() {
        return new ArrayList<>(groupsDB.values());
    }
    
    /**
     * 获取群聊详情
     */
    public ChatGroup getGroupById(Long groupId) {
        return groupsDB.get(groupId);
    }
    
    /**
     * 更新群聊
     */
    public ChatGroup updateGroup(Long groupId, String groupName, String description) {
        ChatGroup group = groupsDB.get(groupId);
        if (group != null) {
            group.setGroupName(groupName);
            group.setDescription(description);
            group.setUpdatedAt(System.currentTimeMillis());
        }
        return group;
    }
    
    /**
     * 删除群聊
     */
    public void deleteGroup(Long groupId) {
        groupsDB.remove(groupId);
        messagesDB.remove(groupId);
    }
    
    /**
     * 添加成员
     */
    public void addMember(Long groupId, String userId) {
        ChatGroup group = groupsDB.get(groupId);
        if (group != null && !group.getMemberIds().contains(userId)) {
            group.getMemberIds().add(userId);
            group.setUpdatedAt(System.currentTimeMillis());
        }
    }
    
    /**
     * 移除成员
     */
    public void removeMember(Long groupId, String userId) {
        ChatGroup group = groupsDB.get(groupId);
        if (group != null) {
            group.getMemberIds().remove(userId);
            group.setUpdatedAt(System.currentTimeMillis());
        }
    }
    
    /**
     * 保存群聊消息
     */
    public GroupMessage saveGroupMessage(Long groupId, String senderId, String content) {
        GroupMessage message = new GroupMessage();
        message.setId(messageIdGenerator++);
        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());
        message.setStatus(GroupMessage.MessageStatus.SENT);
        
        messagesDB.computeIfAbsent(groupId, k -> new ArrayList<>()).add(message);
        return message;
    }
    
    /**
     * 获取群聊消息
     */
    public List<GroupMessage> getGroupMessages(Long groupId) {
        return messagesDB.getOrDefault(groupId, new ArrayList<>());
    }
}
```

### 步骤 3: 创建 REST 控制器

**src/main/java/com/muybaby/websocket/controller/FriendController.java**

```java
package com.muybaby.websocket.controller;

import com.muybaby.websocket.model.Friend;
import com.muybaby.websocket.model.FriendRequest;
import com.muybaby.websocket.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@CrossOrigin(origins = "*")
public class FriendController {
    
    @Autowired
    private FriendService friendService;
    
    /**
     * 发送好友请求
     */
    @PostMapping("/request")
    public ResponseEntity<FriendRequest> sendFriendRequest(@RequestBody Map<String, String> request) {
        String senderId = request.get("senderId");
        String receiverId = request.get("receiverId");
        String message = request.get("message");
        
        FriendRequest friendRequest = friendService.sendFriendRequest(senderId, receiverId, message);
        return ResponseEntity.ok(friendRequest);
    }
    
    /**
     * 获取待接受的好友请求
     */
    @GetMapping("/requests")
    public ResponseEntity<List<FriendRequest>> getPendingRequests(@RequestParam String userId) {
        List<FriendRequest> requests = friendService.getPendingRequests(userId);
        return ResponseEntity.ok(requests);
    }
    
    /**
     * 接受好友请求
     */
    @PutMapping("/requests/{id}/accept")
    public ResponseEntity<Void> acceptFriendRequest(@PathVariable Long id) {
        friendService.acceptFriendRequest(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 拒绝好友请求
     */
    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<Void> rejectFriendRequest(@PathVariable Long id) {
        friendService.rejectFriendRequest(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping
    public ResponseEntity<List<Friend>> getFriends(@RequestParam String userId) {
        List<Friend> friends = friendService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(@RequestParam String userId, @PathVariable String friendId) {
        friendService.removeFriend(userId, friendId);
        return ResponseEntity.ok().build();
    }
}
```

**src/main/java/com/muybaby/websocket/controller/GroupController.java**

```java
package com.muybaby.websocket.controller;

import com.muybaby.websocket.model.ChatGroup;
import com.muybaby.websocket.model.GroupMessage;
import com.muybaby.websocket.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {
    
    @Autowired
    private GroupService groupService;
    
    /**
     * 创建群聊
     */
    @PostMapping
    public ResponseEntity<ChatGroup> createGroup(@RequestBody Map<String, String> request) {
        String groupName = request.get("groupName");
        String description = request.get("description");
        String ownerId = request.get("ownerId");
        
        ChatGroup group = groupService.createGroup(groupName, description, ownerId);
        return ResponseEntity.ok(group);
    }
    
    /**
     * 获取群聊列表
     */
    @GetMapping
    public ResponseEntity<List<ChatGroup>> getGroups() {
        List<ChatGroup> groups = groupService.getGroups();
        return ResponseEntity.ok(groups);
    }
    
    /**
     * 获取群聊详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChatGroup> getGroupById(@PathVariable Long id) {
        ChatGroup group = groupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }
    
    /**
     * 更新群聊
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChatGroup> updateGroup(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String groupName = request.get("groupName");
        String description = request.get("description");
        
        ChatGroup group = groupService.updateGroup(id, groupName, description);
        return ResponseEntity.ok(group);
    }
    
    /**
     * 删除群聊
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 添加成员
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        groupService.addMember(id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 移除成员
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable String userId) {
        groupService.removeMember(id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取群聊消息
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<GroupMessage>> getGroupMessages(@PathVariable Long id) {
        List<GroupMessage> messages = groupService.getGroupMessages(id);
        return ResponseEntity.ok(messages);
    }
}
```

### 步骤 4: 创建 WebSocket 消息处理器

**src/main/java/com/muybaby/websocket/controller/GroupChatController.java**

```java
package com.muybaby.websocket.controller;

import com.muybaby.websocket.model.GroupMessage;
import com.muybaby.websocket.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GroupChatController {
    
    @Autowired
    private GroupService groupService;
    
    /**
     * 发送群聊消息
     * 客户端发送到: /app/group.sendMessage/{groupId}
     * 服务器广播到: /topic/group/{groupId}
     */
    @MessageMapping("/group.sendMessage/{groupId}")
    @SendTo("/topic/group/{groupId}")
    public GroupMessage sendGroupMessage(@DestinationVariable Long groupId,
                                         @Payload GroupMessage message) {
        // 保存消息
        GroupMessage savedMessage = groupService.saveGroupMessage(
                groupId, 
                message.getSenderId(), 
                message.getContent()
        );
        return savedMessage;
    }
    
    /**
     * 添加群成员（系统消息）
     * 客户端发送到: /app/group.addMember/{groupId}
     * 服务器广播到: /topic/group/{groupId}
     */
    @MessageMapping("/group.addMember/{groupId}")
    @SendTo("/topic/group/{groupId}")
    public GroupMessage addMember(@DestinationVariable Long groupId,
                                  @Payload Map<String, String> data) {
        String userId = data.get("userId");
        groupService.addMember(groupId, userId);
        
        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setSenderId("SYSTEM");
        message.setContent(userId + " 加入了群聊");
        message.setTimestamp(System.currentTimeMillis());
        message.setStatus(GroupMessage.MessageStatus.SENT);
        
        return message;
    }
}
```

---

## 前端实现步骤

### 步骤 1: 扩展 WebSocket Composable

**webSocketFront/src/composables/useWebSocket.ts (补充)**

```typescript
// 添加以下内容到现有的 useWebSocket.ts

export interface PrivateMessage {
    senderId: string
    receiverId: string
    content: string
    timestamp: number
    isRead: boolean
}

export interface GroupMessage {
    id: number
    groupId: number
    senderId: string
    content: string
    timestamp: number
    status: 'SENT' | 'DELIVERED' | 'READ'
}

export interface ChatGroup {
    id: number
    groupName: string
    description: string
    ownerId: string
    memberIds: string[]
    createdAt: number
    updatedAt: number
}

export function useWebSocket() {
    // ... 现有代码 ...
    
    const privateMessages = ref<Map<string, PrivateMessage[]>>(new Map())
    const groups = ref<ChatGroup[]>([])
    const groupMessages = ref<Map<number, GroupMessage[]>>(new Map())
    
    /**
     * 订阅私聊频道
     */
    const subscribeToPrivateChat = (friendId: string) => {
        if (stompClient.value && connected.value) {
            stompClient.value.subscribe(
                `/user/queue/private-chat/${friendId}`,
                (message: IMessage) => {
                    const privateMessage: PrivateMessage = JSON.parse(message.body)
                    if (!privateMessages.value.has(friendId)) {
                        privateMessages.value.set(friendId, [])
                    }
                    privateMessages.value.get(friendId)?.push(privateMessage)
                }
            )
        }
    }
    
    /**
     * 发送私聊消息
     */
    const sendPrivateMessage = (receiverId: string, content: string) => {
        if (stompClient.value && connected.value) {
            const message: PrivateMessage = {
                senderId: currentUser.value,
                receiverId: receiverId,
                content: content,
                timestamp: Date.now(),
                isRead: false
            }
            
            stompClient.value.publish({
                destination: `/app/private.sendMessage/${receiverId}`,
                body: JSON.stringify(message)
            })
        }
    }
    
    /**
     * 订阅群聊频道
     */
    const subscribeToGroup = (groupId: number) => {
        if (stompClient.value && connected.value) {
            stompClient.value.subscribe(
                `/topic/group/${groupId}`,
                (message: IMessage) => {
                    const groupMessage: GroupMessage = JSON.parse(message.body)
                    if (!groupMessages.value.has(groupId)) {
                        groupMessages.value.set(groupId, [])
                    }
                    groupMessages.value.get(groupId)?.push(groupMessage)
                }
            )
        }
    }
    
    /**
     * 发送群聊消息
     */
    const sendGroupMessage = (groupId: number, content: string) => {
        if (stompClient.value && connected.value) {
            const message: GroupMessage = {
                id: 0,
                groupId: groupId,
                senderId: currentUser.value,
                content: content,
                timestamp: Date.now(),
                status: 'SENT'
            }
            
            stompClient.value.publish({
                destination: `/app/group.sendMessage/${groupId}`,
                body: JSON.stringify(message)
            })
        }
    }
    
    return {
        // ... 现有返回值 ...
        privateMessages,
        groups,
        groupMessages,
        subscribeToPrivateChat,
        sendPrivateMessage,
        subscribeToGroup,
        sendGroupMessage
    }
}
```

### 步骤 2: 创建好友面板组件

**webSocketFront/src/components/FriendPanel.vue**

```vue
<template>
    <div class="friend-panel">
        <div class="friend-header">
            <h2>好友列表</h2>
            <button @click="showAddFriendDialog = true" class="btn-add">+</button>
        </div>
        
        <div class="tabs">
            <button 
                :class="{ active: activeTab === 'friends' }"
                @click="activeTab = 'friends'"
            >好友 ({{ friends.length }})</button>
            <button 
                :class="{ active: activeTab === 'requests' }"
                @click="activeTab = 'requests'"
            >请求 ({{ friendRequests.length }})</button>
        </div>
        
        <!-- 好友列表 -->
        <div v-if="activeTab === 'friends'" class="friend-list">
            <div v-for="friend in friends" :key="friend.friendId" class="friend-item">
                <div class="friend-info">
                    <div class="friend-name">{{ friend.friendName }}</div>
                    <div :class="['friend-status', onlineFriends.includes(friend.friendId) ? 'online' : 'offline']">
                        {{ onlineFriends.includes(friend.friendId) ? '在线' : '离线' }}
                    </div>
                </div>
                <div class="friend-actions">
                    <button @click="startChat(friend.friendId)" class="btn-chat">聊天</button>
                    <button @click="removeFriend(friend.friendId)" class="btn-delete">删除</button>
                </div>
            </div>
        </div>
        
        <!-- 好友请求 -->
        <div v-if="activeTab === 'requests'" class="requests-list">
            <div v-for="request in friendRequests" :key="request.id" class="request-item">
                <div class="request-info">
                    <div class="request-sender">{{ request.senderId }}</div>
                    <div class="request-message">{{ request.message }}</div>
                </div>
                <div class="request-actions">
                    <button @click="acceptRequest(request.id)" class="btn-accept">接受</button>
                    <button @click="rejectRequest(request.id)" class="btn-reject">拒绝</button>
                </div>
            </div>
        </div>
        
        <!-- 添加好友对话框 -->
        <div v-if="showAddFriendDialog" class="dialog-overlay">
            <div class="dialog">
                <h3>添加好友</h3>
                <input 
                    v-model="newFriendId" 
                    type="text" 
                    placeholder="输入好友用户名"
                    @keyup.enter="sendFriendRequest"
                >
                <textarea 
                    v-model="friendRequestMessage" 
                    placeholder="输入请求消息（可选）"
                ></textarea>
                <div class="dialog-actions">
                    <button @click="sendFriendRequest" class="btn-primary">发送请求</button>
                    <button @click="showAddFriendDialog = false" class="btn-secondary">取消</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { Friend, FriendRequest } from '../types'

const friends = ref<Friend[]>([])
const friendRequests = ref<FriendRequest[]>([])
const onlineFriends = ref<string[]>([])
const activeTab = ref('friends')
const showAddFriendDialog = ref(false)
const newFriendId = ref('')
const friendRequestMessage = ref('')
const currentUser = ref('')

onMounted(() => {
    loadFriends()
    loadFriendRequests()
})

const loadFriends = async () => {
    try {
        const response = await fetch(`/api/friends?userId=${currentUser.value}`)
        friends.value = await response.json()
    } catch (error) {
        console.error('加载好友列表失败:', error)
    }
}

const loadFriendRequests = async () => {
    try {
        const response = await fetch(`/api/friends/requests?userId=${currentUser.value}`)
        friendRequests.value = await response.json()
    } catch (error) {
        console.error('加载好友请求失败:', error)
    }
}

const sendFriendRequest = async () => {
    if (!newFriendId.value) return
    
    try {
        await fetch('/api/friends/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                senderId: currentUser.value,
                receiverId: newFriendId.value,
                message: friendRequestMessage.value
            })
        })
        newFriendId.value = ''
        friendRequestMessage.value = ''
        showAddFriendDialog.value = false
    } catch (error) {
        console.error('发送好友请求失败:', error)
    }
}

const acceptRequest = async (requestId: number) => {
    try {
        await fetch(`/api/friends/requests/${requestId}/accept`, { method: 'PUT' })
        loadFriendRequests()
        loadFriends()
    } catch (error) {
        console.error('接受好友请求失败:', error)
    }
}

const rejectRequest = async (requestId: number) => {
    try {
        await fetch(`/api/friends/requests/${requestId}/reject`, { method: 'PUT' })
        loadFriendRequests()
    } catch (error) {
        console.error('拒绝好友请求失败:', error)
    }
}

const removeFriend = async (friendId: string) => {
    try {
        await fetch(`/api/friends/${friendId}?userId=${currentUser.value}`, { 
            method: 'DELETE' 
        })
        loadFriends()
    } catch (error) {
        console.error('删除好友失败:', error)
    }
}

const startChat = (friendId: string) => {
    // 触发私聊事件
    window.dispatchEvent(new CustomEvent('startPrivateChat', { 
        detail: { friendId } 
    }))
}
</script>

<style scoped>
.friend-panel {
    border-radius: 8px;
    background: white;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.friend-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
}

.tabs button {
    padding: 8px 16px;
    border: 1px solid #ddd;
    background: white;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;
}

.tabs button.active {
    background: #007bff;
    color: white;
    border-color: #007bff;
}

.friend-list, .requests-list {
    max-height: 400px;
    overflow-y: auto;
}

.friend-item, .request-item {
    padding: 12px;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.friend-info, .request-info {
    flex: 1;
}

.friend-name, .request-sender {
    font-weight: 500;
}

.friend-status {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 12px;
    margin-top: 4px;
}

.friend-status.online {
    background: #d4edda;
    color: #155724;
}

.friend-status.offline {
    background: #f8f9fa;
    color: #6c757d;
}

.dialog-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}

.dialog {
    background: white;
    padding: 24px;
    border-radius: 8px;
    width: 90%;
    max-width: 400px;
}

.dialog input, .dialog textarea {
    width: 100%;
    padding: 8px;
    margin: 8px 0;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-family: inherit;
}

.btn-add, .btn-primary, .btn-secondary, .btn-chat, .btn-accept, .btn-reject, .btn-delete {
    padding: 6px 12px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-primary { background: #007bff; color: white; }
.btn-secondary { background: #6c757d; color: white; }
.btn-chat { background: #28a745; color: white; }
.btn-accept { background: #28a745; color: white; }
.btn-reject { background: #dc3545; color: white; }
.btn-delete { background: #dc3545; color: white; }
</style>
```

### 步骤 3: 创建群聊管理组件

**webSocketFront/src/components/GroupManager.vue**

```vue
<template>
    <div class="group-manager">
        <div class="group-header">
            <h2>群聊管理</h2>
            <button @click="showCreateGroupDialog = true" class="btn-create">新建群聊</button>
        </div>
        
        <div class="groups-grid">
            <div v-for="group in groups" :key="group.id" class="group-card">
                <div class="group-name">{{ group.groupName }}</div>
                <div class="group-description">{{ group.description }}</div>
                <div class="group-members">成员: {{ group.memberIds.length }}</div>
                <div class="group-actions">
                    <button @click="joinGroup(group.id)" class="btn-join">加入</button>
                    <button @click="selectGroup(group)" class="btn-manage">管理</button>
                </div>
            </div>
        </div>
        
        <!-- 创建群聊对话框 -->
        <div v-if="showCreateGroupDialog" class="dialog-overlay">
            <div class="dialog">
                <h3>创建群聊</h3>
                <input v-model="newGroup.name" type="text" placeholder="群名称" />
                <textarea v-model="newGroup.description" placeholder="群描述"></textarea>
                <div class="dialog-actions">
                    <button @click="createGroup" class="btn-primary">创建</button>
                    <button @click="showCreateGroupDialog = false" class="btn-secondary">取消</button>
                </div>
            </div>
        </div>
        
        <!-- 群聊管理对话框 -->
        <div v-if="selectedGroup && showGroupManageDialog" class="dialog-overlay">
            <div class="dialog">
                <h3>管理群聊: {{ selectedGroup.groupName }}</h3>
                
                <div class="members-section">
                    <h4>成员列表</h4>
                    <div class="members-list">
                        <div v-for="member in selectedGroup.memberIds" :key="member" class="member-item">
                            <span>{{ member }}</span>
                            <button v-if="canRemoveMember(member)" @click="removeMember(member)" class="btn-remove">
                                移除
                            </button>
                        </div>
                    </div>
                </div>
                
                <div class="add-member-section">
                    <h4>添加成员</h4>
                    <input v-model="newMemberId" type="text" placeholder="输入成员用户名" />
                    <button @click="addMember" class="btn-primary">添加</button>
                </div>
                
                <div class="dialog-actions">
                    <button 
                        v-if="isGroupOwner" 
                        @click="deleteGroup" 
                        class="btn-danger"
                    >删除群聊</button>
                    <button @click="showGroupManageDialog = false" class="btn-secondary">关闭</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { ChatGroup } from '../types'

const groups = ref<ChatGroup[]>([])
const selectedGroup = ref<ChatGroup | null>(null)
const showCreateGroupDialog = ref(false)
const showGroupManageDialog = ref(false)
const currentUser = ref('')
const newGroup = ref({ name: '', description: '' })
const newMemberId = ref('')

const isGroupOwner = computed(() => {
    return selectedGroup.value?.ownerId === currentUser.value
})

onMounted(() => {
    loadGroups()
})

const loadGroups = async () => {
    try {
        const response = await fetch('/api/groups')
        groups.value = await response.json()
    } catch (error) {
        console.error('加载群聊列表失败:', error)
    }
}

const createGroup = async () => {
    if (!newGroup.value.name) return
    
    try {
        await fetch('/api/groups', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                groupName: newGroup.value.name,
                description: newGroup.value.description,
                ownerId: currentUser.value
            })
        })
        newGroup.value = { name: '', description: '' }
        showCreateGroupDialog.value = false
        loadGroups()
    } catch (error) {
        console.error('创建群聊失败:', error)
    }
}

const joinGroup = async (groupId: number) => {
    try {
        await fetch(`/api/groups/${groupId}/members`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.value })
        })
        loadGroups()
    } catch (error) {
        console.error('加入群聊失败:', error)
    }
}

const selectGroup = (group: ChatGroup) => {
    selectedGroup.value = group
    showGroupManageDialog.value = true
}

const addMember = async () => {
    if (!selectedGroup.value || !newMemberId.value) return
    
    try {
        await fetch(`/api/groups/${selectedGroup.value.id}/members`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: newMemberId.value })
        })
        newMemberId.value = ''
        loadGroups()
    } catch (error) {
        console.error('添加成员失败:', error)
    }
}

const removeMember = async (userId: string) => {
    if (!selectedGroup.value) return
    
    try {
        await fetch(`/api/groups/${selectedGroup.value.id}/members/${userId}`, {
            method: 'DELETE'
        })
        loadGroups()
    } catch (error) {
        console.error('移除成员失败:', error)
    }
}

const deleteGroup = async () => {
    if (!selectedGroup.value) return
    if (!confirm('确定要删除这个群聊吗?')) return
    
    try {
        await fetch(`/api/groups/${selectedGroup.value.id}`, {
            method: 'DELETE'
        })
        showGroupManageDialog.value = false
        selectedGroup.value = null
        loadGroups()
    } catch (error) {
        console.error('删除群聊失败:', error)
    }
}

const canRemoveMember = (memberId: string) => {
    return isGroupOwner.value && memberId !== selectedGroup.value?.ownerId
}
</script>

<style scoped>
.group-manager {
    padding: 16px;
}

.group-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.groups-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 12px;
}

.group-card {
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 12px;
    background: white;
}

.group-name {
    font-weight: 500;
    font-size: 14px;
    margin-bottom: 4px;
}

.group-description {
    font-size: 12px;
    color: #666;
    margin-bottom: 8px;
}

.group-members {
    font-size: 12px;
    color: #999;
    margin-bottom: 8px;
}

.group-actions {
    display: flex;
    gap: 6px;
}

.members-section, .add-member-section {
    margin: 12px 0;
}

.members-list {
    border: 1px solid #ddd;
    border-radius: 4px;
    max-height: 200px;
    overflow-y: auto;
}

.member-item {
    padding: 8px;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

/* 按钮样式 */
.btn-create, .btn-primary, .btn-secondary, .btn-join, .btn-manage, .btn-remove, .btn-danger {
    padding: 6px 12px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
}

.btn-create { background: #007bff; color: white; }
.btn-primary { background: #28a745; color: white; }
.btn-secondary { background: #6c757d; color: white; }
.btn-join { background: #17a2b8; color: white; }
.btn-manage { background: #ffc107; color: black; }
.btn-remove { background: #dc3545; color: white; }
.btn-danger { background: #dc3545; color: white; }

.dialog-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}

.dialog {
    background: white;
    padding: 24px;
    border-radius: 8px;
    width: 90%;
    max-width: 500px;
    max-height: 80vh;
    overflow-y: auto;
}
</style>
```

---

## 完整示例代码

完整的实现包括前后端类型定义、更多功能等，详见本指南各部分。

---

## 测试指南

### 好友系统测试

1. **发送好友请求**
   ```bash
   curl -X POST http://localhost:8080/api/friends/request \
     -H "Content-Type: application/json" \
     -d '{"senderId":"Alice","receiverId":"Bob","message":"我们可以聊天吗?"}'
   ```

2. **获取待接受的好友请求**
   ```bash
   curl http://localhost:8080/api/friends/requests?userId=Bob
   ```

3. **接受好友请求**
   ```bash
   curl -X PUT http://localhost:8080/api/friends/requests/1/accept
   ```

### 群聊系统测试

1. **创建群聊**
   ```bash
   curl -X POST http://localhost:8080/api/groups \
     -H "Content-Type: application/json" \
     -d '{"groupName":"Java学习","description":"讨论Java技术","ownerId":"Alice"}'
   ```

2. **获取群聊列表**
   ```bash
   curl http://localhost:8080/api/groups
   ```

3. **添加群成员**
   ```bash
   curl -X POST http://localhost:8080/api/groups/1/members \
     -H "Content-Type: application/json" \
     -d '{"userId":"Bob"}'
   ```

4. **发送群聊消息**
   - 通过前端 UI 或 WebSocket 发送
   - 观察所有群成员是否都能收到消息

---

## 总结

这个实现方案涵盖了：

✅ **好友管理** - 添加、接受、拒绝、删除好友
✅ **私聊功能** - 用户间的一对一通信  
✅ **群聊CRUD** - 创建、读取、更新、删除群聊
✅ **群成员管理** - 添加、移除、管理群成员
✅ **实时同步** - 通过 WebSocket 实现实时消息推送
✅ **REST API** - 提供完整的 CRUD 接口
✅ **前端组件** - 好友面板、群聊管理等 UI 组件

下一步可以考虑：
- [ ] 集成真实数据库（MySQL/PostgreSQL）
- [ ] 添加用户认证（Spring Security）
- [ ] 消息存储和历史查询
- [ ] 文件上传功能
- [ ] 视频/语音通话
- [ ] 消息加密

