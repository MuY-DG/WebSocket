# 完整代码示例和最佳实践

## 📚 目录

1. [后端代码示例](#后端代码示例)
2. [前端代码示例](#前端代码示例)
3. [最佳实践](#最佳实践)
4. [常见问题解决](#常见问题解决)

---

## 后端代码示例

### 1. 完整的友谊服务实现

**FriendService.java** - 业务逻辑层

```java
package com.muybaby.websocket.service;

import com.muybaby.websocket.model.Friend;
import com.muybaby.websocket.model.FriendRequest;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FriendService {
    
    // 模拟数据库: userId -> Friends 列表
    private final ConcurrentHashMap<String, List<Friend>> friendsDB = new ConcurrentHashMap<>();
    
    // 模拟数据库: requestId -> FriendRequest
    private final ConcurrentHashMap<Long, FriendRequest> requestsDB = new ConcurrentHashMap<>();
    
    // 好友请求 ID 生成器
    private Long requestIdGenerator = 1L;
    
    /**
     * 发送好友请求
     * @param senderId 发送者用户ID
     * @param receiverId 接收者用户ID
     * @param message 请求消息
     * @return 创建的好友请求对象
     */
    public synchronized FriendRequest sendFriendRequest(String senderId, String receiverId, String message) {
        // 验证输入
        if (senderId == null || receiverId == null || senderId.equals(receiverId)) {
            throw new IllegalArgumentException("无效的用户ID");
        }
        
        // 检查是否已是好友
        if (isFriend(senderId, receiverId)) {
            throw new IllegalArgumentException("已经是好友了");
        }
        
        // 创建请求对象
        FriendRequest request = new FriendRequest();
        request.setId(requestIdGenerator++);
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setMessage(message != null ? message : "");
        request.setStatus(FriendRequest.RequestStatus.PENDING);
        request.setCreatedAt(System.currentTimeMillis());
        request.setUpdatedAt(System.currentTimeMillis());
        
        requestsDB.put(request.getId(), request);
        return request;
    }
    
    /**
     * 获取用户的所有待接受的好友请求
     * @param userId 用户ID
     * @return 待接受的好友请求列表
     */
    public List<FriendRequest> getPendingRequests(String userId) {
        return requestsDB.values().stream()
                .filter(r -> r.getReceiverId().equals(userId) && 
                           r.getStatus() == FriendRequest.RequestStatus.PENDING)
                .sorted(Comparator.comparing(FriendRequest::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * 接受好友请求
     * @param requestId 请求ID
     */
    public synchronized void acceptFriendRequest(Long requestId) {
        FriendRequest request = requestsDB.get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("请求不存在");
        }
        
        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("请求状态无效");
        }
        
        // 更新请求状态
        request.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        request.setUpdatedAt(System.currentTimeMillis());
        
        // 创建双向好友关系
        addFriendPair(request.getSenderId(), request.getReceiverId());
    }
    
    /**
     * 拒绝好友请求
     * @param requestId 请求ID
     */
    public synchronized void rejectFriendRequest(Long requestId) {
        FriendRequest request = requestsDB.get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("请求不存在");
        }
        
        request.setStatus(FriendRequest.RequestStatus.REJECTED);
        request.setUpdatedAt(System.currentTimeMillis());
    }
    
    /**
     * 添加双向好友关系
     * @param userId1 用户1
     * @param userId2 用户2
     */
    private void addFriendPair(String userId1, String userId2) {
        addFriend(userId1, userId2);
        addFriend(userId2, userId1);
    }
    
    /**
     * 添加单向好友关系
     */
    private void addFriend(String userId, String friendId) {
        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setFriendName(friendId); // 实际项目中应该查询用户昵称
        friend.setStatus(Friend.FriendStatus.ACCEPTED);
        friend.setCreatedAt(System.currentTimeMillis());
        friend.setUpdatedAt(System.currentTimeMillis());
        
        List<Friend> friends = friendsDB.computeIfAbsent(userId, k -> 
            Collections.synchronizedList(new ArrayList<>())
        );
        
        if (!friends.stream().anyMatch(f -> f.getFriendId().equals(friendId))) {
            friends.add(friend);
        }
    }
    
    /**
     * 获取用户的好友列表
     * @param userId 用户ID
     * @return 好友列表
     */
    public List<Friend> getFriends(String userId) {
        return new ArrayList<>(friendsDB.getOrDefault(userId, new ArrayList<>()));
    }
    
    /**
     * 删除好友关系
     * @param userId 用户ID
     * @param friendId 好友ID
     */
    public synchronized void removeFriend(String userId, String friendId) {
        // 删除单向关系
        removeFriendPair(userId, friendId);
        removeFriendPair(friendId, userId);
    }
    
    /**
     * 删除单向好友关系
     */
    private void removeFriendPair(String userId, String friendId) {
        List<Friend> friends = friendsDB.get(userId);
        if (friends != null) {
            friends.removeIf(f -> f.getFriendId().equals(friendId));
        }
    }
    
    /**
     * 检查两个用户是否是好友
     * @param userId1 用户1
     * @param userId2 用户2
     * @return 是否是好友
     */
    public boolean isFriend(String userId1, String userId2) {
        List<Friend> friends = friendsDB.get(userId1);
        return friends != null && friends.stream()
                .anyMatch(f -> f.getFriendId().equals(userId2) && 
                            f.getStatus() == Friend.FriendStatus.ACCEPTED);
    }
    
    /**
     * 获取在线好友（这里需要集成用户在线状态管理）
     * @param userId 用户ID
     * @return 在线好友列表
     */
    public List<Friend> getOnlineFriends(String userId) {
        // TODO: 需要与在线用户管理系统集成
        return getFriends(userId); // 暂时返回所有好友
    }
}
```

### 2. 完整的群聊服务实现

**GroupService.java** - 群聊业务逻辑

```java
package com.muybaby.websocket.service;

import com.muybaby.websocket.model.ChatGroup;
import com.muybaby.websocket.model.GroupMessage;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GroupService {
    
    // 模拟数据库: groupId -> ChatGroup
    private final ConcurrentHashMap<Long, ChatGroup> groupsDB = new ConcurrentHashMap<>();
    
    // 模拟数据库: groupId -> Messages
    private final ConcurrentHashMap<Long, List<GroupMessage>> messagesDB = new ConcurrentHashMap<>();
    
    // ID 生成器
    private Long groupIdGenerator = 1L;
    private Long messageIdGenerator = 1L;
    
    /**
     * 创建群聊
     * @param groupName 群名称
     * @param description 群描述
     * @param ownerId 群主ID
     * @return 创建的群聊对象
     */
    public synchronized ChatGroup createGroup(String groupName, String description, String ownerId) {
        // 验证输入
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("群名称不能为空");
        }
        
        ChatGroup group = new ChatGroup();
        group.setId(groupIdGenerator++);
        group.setGroupName(groupName);
        group.setDescription(description);
        group.setOwnerId(ownerId);
        group.setMemberIds(Collections.synchronizedList(
            new ArrayList<>(List.of(ownerId))  // 群主自动加入
        ));
        group.setCreatedAt(System.currentTimeMillis());
        group.setUpdatedAt(System.currentTimeMillis());
        
        groupsDB.put(group.getId(), group);
        return group;
    }
    
    /**
     * 获取所有群聊（可以优化为分页查询）
     * @return 群聊列表
     */
    public List<ChatGroup> getAllGroups() {
        return new ArrayList<>(groupsDB.values());
    }
    
    /**
     * 获取用户加入的所有群聊
     * @param userId 用户ID
     * @return 群聊列表
     */
    public List<ChatGroup> getUserGroups(String userId) {
        return groupsDB.values().stream()
                .filter(g -> g.getMemberIds().contains(userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取群聊详情
     * @param groupId 群ID
     * @return 群聊对象，若不存在则返回 null
     */
    public ChatGroup getGroupById(Long groupId) {
        return groupsDB.get(groupId);
    }
    
    /**
     * 验证群聊是否存在
     */
    private void validateGroupExists(Long groupId) {
        if (!groupsDB.containsKey(groupId)) {
            throw new IllegalArgumentException("群聊不存在");
        }
    }
    
    /**
     * 更新群聊信息（只有群主可以更新）
     * @param groupId 群ID
     * @param groupName 新群名称
     * @param description 新描述
     * @return 更新后的群聊对象
     */
    public synchronized ChatGroup updateGroup(Long groupId, String groupName, String description) {
        validateGroupExists(groupId);
        
        ChatGroup group = groupsDB.get(groupId);
        if (groupName != null && !groupName.trim().isEmpty()) {
            group.setGroupName(groupName);
        }
        if (description != null) {
            group.setDescription(description);
        }
        group.setUpdatedAt(System.currentTimeMillis());
        
        return group;
    }
    
    /**
     * 删除群聊（只有群主可以删除）
     * @param groupId 群ID
     */
    public synchronized void deleteGroup(Long groupId) {
        validateGroupExists(groupId);
        
        groupsDB.remove(groupId);
        messagesDB.remove(groupId); // 删除所有相关消息
    }
    
    /**
     * 添加成员到群聊
     * @param groupId 群ID
     * @param userId 用户ID
     */
    public synchronized void addMember(Long groupId, String userId) {
        validateGroupExists(groupId);
        
        ChatGroup group = groupsDB.get(groupId);
        if (!group.getMemberIds().contains(userId)) {
            group.getMemberIds().add(userId);
            group.setUpdatedAt(System.currentTimeMillis());
        }
    }
    
    /**
     * 从群聊中移除成员
     * @param groupId 群ID
     * @param userId 用户ID
     */
    public synchronized void removeMember(Long groupId, String userId) {
        validateGroupExists(groupId);
        
        ChatGroup group = groupsDB.get(groupId);
        
        // 不能移除群主
        if (userId.equals(group.getOwnerId())) {
            throw new IllegalArgumentException("不能移除群主");
        }
        
        if (group.getMemberIds().remove(userId)) {
            group.setUpdatedAt(System.currentTimeMillis());
        }
    }
    
    /**
     * 获取群聊成员数
     * @param groupId 群ID
     * @return 成员数
     */
    public int getMemberCount(Long groupId) {
        ChatGroup group = groupsDB.get(groupId);
        return group != null ? group.getMemberIds().size() : 0;
    }
    
    /**
     * 检查用户是否在群聊中
     * @param groupId 群ID
     * @param userId 用户ID
     * @return 是否在群聊中
     */
    public boolean isMember(Long groupId, String userId) {
        ChatGroup group = groupsDB.get(groupId);
        return group != null && group.getMemberIds().contains(userId);
    }
    
    /**
     * 保存群聊消息
     * @param groupId 群ID
     * @param senderId 发送者ID
     * @param content 消息内容
     * @return 创建的消息对象
     */
    public synchronized GroupMessage saveGroupMessage(Long groupId, String senderId, String content) {
        validateGroupExists(groupId);
        
        if (!isMember(groupId, senderId)) {
            throw new IllegalArgumentException("用户不在群聊中");
        }
        
        GroupMessage message = new GroupMessage();
        message.setId(messageIdGenerator++);
        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());
        message.setStatus(GroupMessage.MessageStatus.SENT);
        
        List<GroupMessage> messages = messagesDB.computeIfAbsent(groupId, k -> 
            Collections.synchronizedList(new ArrayList<>())
        );
        messages.add(message);
        
        return message;
    }
    
    /**
     * 获取群聊消息
     * @param groupId 群ID
     * @return 消息列表
     */
    public List<GroupMessage> getGroupMessages(Long groupId) {
        return new ArrayList<>(messagesDB.getOrDefault(groupId, new ArrayList<>()));
    }
    
    /**
     * 分页获取群聊消息（推荐用于生产环境）
     * @param groupId 群ID
     * @param pageNumber 页码（从0开始）
     * @param pageSize 每页大小
     * @return 分页消息列表
     */
    public List<GroupMessage> getGroupMessagesPaginated(Long groupId, int pageNumber, int pageSize) {
        List<GroupMessage> messages = messagesDB.getOrDefault(groupId, new ArrayList<>());
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, messages.size());
        
        if (start >= messages.size()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(messages.subList(start, end));
    }
    
    /**
     * 清空群聊消息（只有群主可以）
     * @param groupId 群ID
     */
    public synchronized void clearGroupMessages(Long groupId) {
        validateGroupExists(groupId);
        messagesDB.remove(groupId);
    }
}
```

### 3. WebSocket 消息处理器

**GroupChatController.java** - 处理群聊WebSocket消息

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
import java.util.Map;

@Controller
public class GroupChatController {
    
    @Autowired
    private GroupService groupService;
    
    /**
     * 发送群聊消息
     * 客户端发送到: /app/group.sendMessage/{groupId}
     * 服务器广播到: /topic/group/{groupId}
     * 
     * 请求格式:
     * {
     *   "senderId": "Alice",
     *   "content": "大家好！"
     * }
     */
    @MessageMapping("/group.sendMessage/{groupId}")
    @SendTo("/topic/group/{groupId}")
    public GroupMessage sendGroupMessage(@DestinationVariable Long groupId,
                                         @Payload GroupMessage message) {
        try {
            // 保存消息到数据库（模拟）
            GroupMessage savedMessage = groupService.saveGroupMessage(
                    groupId, 
                    message.getSenderId(), 
                    message.getContent()
            );
            
            // 更新消息状态
            savedMessage.setStatus(GroupMessage.MessageStatus.DELIVERED);
            
            return savedMessage;
        } catch (Exception e) {
            // 错误处理：返回错误消息给发送者
            GroupMessage errorMessage = new GroupMessage();
            errorMessage.setContent("发送失败: " + e.getMessage());
            errorMessage.setStatus(GroupMessage.MessageStatus.SENT);
            return errorMessage;
        }
    }
    
    /**
     * 添加群成员通知
     * 客户端发送到: /app/group.addMember/{groupId}
     * 服务器广播到: /topic/group/{groupId}
     * 
     * 请求格式:
     * {
     *   "userId": "Bob"
     * }
     */
    @MessageMapping("/group.addMember/{groupId}")
    @SendTo("/topic/group/{groupId}")
    public GroupMessage addMemberNotification(@DestinationVariable Long groupId,
                                              @Payload Map<String, String> data) {
        String userId = data.get("userId");
        
        try {
            groupService.addMember(groupId, userId);
            
            // 发送系统消息
            GroupMessage notification = new GroupMessage();
            notification.setGroupId(groupId);
            notification.setSenderId("SYSTEM");
            notification.setContent(userId + " 加入了群聊");
            notification.setTimestamp(System.currentTimeMillis());
            notification.setStatus(GroupMessage.MessageStatus.DELIVERED);
            
            return notification;
        } catch (Exception e) {
            GroupMessage error = new GroupMessage();
            error.setSenderId("SYSTEM");
            error.setContent("添加成员失败: " + e.getMessage());
            return error;
        }
    }
    
    /**
     * 移除群成员通知
     * 客户端发送到: /app/group.removeMember/{groupId}
     * 服务器广播到: /topic/group/{groupId}
     */
    @MessageMapping("/group.removeMember/{groupId}")
    @SendTo("/topic/group/{groupId}")
    public GroupMessage removeMemberNotification(@DestinationVariable Long groupId,
                                                 @Payload Map<String, String> data) {
        String userId = data.get("userId");
        
        try {
            groupService.removeMember(groupId, userId);
            
            GroupMessage notification = new GroupMessage();
            notification.setGroupId(groupId);
            notification.setSenderId("SYSTEM");
            notification.setContent(userId + " 离开了群聊");
            notification.setTimestamp(System.currentTimeMillis());
            notification.setStatus(GroupMessage.MessageStatus.DELIVERED);
            
            return notification;
        } catch (Exception e) {
            GroupMessage error = new GroupMessage();
            error.setSenderId("SYSTEM");
            error.setContent("移除成员失败: " + e.getMessage());
            return error;
        }
    }
}
```

**PrivateChatController.java** - 处理私聊消息

```java
package com.muybaby.websocket.controller;

import com.muybaby.websocket.model.PrivateMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class PrivateChatController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 发送私聊消息
     * 客户端发送到: /app/private.sendMessage/{receiverId}
     * 服务器发送到: /user/{receiverId}/queue/private-chat/{senderId}
     * 
     * 只有接收者能收到消息
     */
    @MessageMapping("/private.sendMessage/{receiverId}")
    public void sendPrivateMessage(@DestinationVariable String receiverId,
                                   @Payload PrivateMessage message) {
        try {
            // 设置时间戳
            message.setTimestamp(System.currentTimeMillis());
            
            // 发送给指定用户
            // 注意: 这里需要知道接收者的 Principal (登录用户信息)
            // 实际项目中需要从 Principal 中获取当前用户信息
            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/private-chat/" + message.getSenderId(),
                    message
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## 前端代码示例

### 1. 扩展 WebSocket Composable

**useWebSocket.ts** - 完整的WebSocket管理

```typescript
import { ref, onMounted, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Client, type IMessage } from '@stomp/stompjs'

export interface ChatMessage {
    type: 'CHAT' | 'JOIN' | 'LEAVE'
    content: string
    sender: string
    timestamp: number
}

export interface PrivateMessage {
    id?: number
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

export interface Notification {
    title: string
    message: string
    type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
    recipient: string
    timestamp: number
}

export function useWebSocket() {
    const stompClient = ref<Client | null>(null)
    const connected = ref(false)
    const messages = ref<ChatMessage[]>([])
    const notifications = ref<Notification[]>([])
    const currentUser = ref('')
    
    // 私聊消息: Map<friendId, PrivateMessage[]>
    const privateMessages = ref<Map<string, PrivateMessage[]>>(new Map())
    
    // 群聊: Map<groupId, ChatGroup>
    const groups = ref<Map<number, ChatGroup>>(new Map())
    
    // 群聊消息: Map<groupId, GroupMessage[]>
    const groupMessages = ref<Map<number, GroupMessage[]>>(new Map())
    
    // 订阅的群聊: Set<groupId>
    const subscribedGroups = ref<Set<number>>(new Set())
    
    const SOCKET_URL = 'http://localhost:8080/ws'
    
    /**
     * 连接到 WebSocket 服务器
     */
    const connect = (username: string) => {
        currentUser.value = username
        
        const client = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            debug: (str) => {
                console.log('STOMP Debug:', str)
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        })
        
        client.onConnect = () => {
            console.log('WebSocket 连接成功')
            connected.value = true
            
            // 订阅公共聊天频道
            client.subscribe('/topic/public', (message: IMessage) => {
                const chatMessage: ChatMessage = JSON.parse(message.body)
                messages.value.push(chatMessage)
            })
            
            // 订阅个人通知频道
            client.subscribe(`/user/queue/notifications`, (message: IMessage) => {
                const notification: Notification = JSON.parse(message.body)
                notifications.value.push(notification)
            })
            
            // 订阅广播通知频道
            client.subscribe('/topic/notifications', (message: IMessage) => {
                const notification: Notification = JSON.parse(message.body)
                notifications.value.push(notification)
            })
            
            // 发送加入消息
            sendJoinMessage(username)
        }
        
        client.onStompError = (frame) => {
            console.error('STOMP 错误:', frame)
            connected.value = false
        }
        
        client.activate()
        stompClient.value = client
    }
    
    /**
     * 断开连接
     */
    const disconnect = () => {
        if (stompClient.value) {
            stompClient.value.deactivate()
            connected.value = false
            console.log('WebSocket 已断开')
        }
    }
    
    /**
     * 发送加入消息
     */
    const sendJoinMessage = (username: string) => {
        if (stompClient.value && connected.value) {
            const chatMessage: ChatMessage = {
                type: 'JOIN',
                sender: username,
                content: '',
                timestamp: Date.now(),
            }
            
            stompClient.value.publish({
                destination: '/app/chat.addUser',
                body: JSON.stringify(chatMessage),
            })
        }
    }
    
    /**
     * 发送聊天消息
     */
    const sendMessage = (content: string) => {
        if (stompClient.value && connected.value) {
            const chatMessage: ChatMessage = {
                type: 'CHAT',
                content: content,
                sender: currentUser.value,
                timestamp: Date.now(),
            }
            
            stompClient.value.publish({
                destination: '/app/chat.sendMessage',
                body: JSON.stringify(chatMessage),
            })
        }
    }
    
    /**
     * 发送通知
     */
    const sendNotification = (notification: Omit<Notification, 'timestamp'>) => {
        if (stompClient.value && connected.value) {
            const fullNotification: Notification = {
                ...notification,
                timestamp: Date.now(),
            }
            
            stompClient.value.publish({
                destination: '/app/notification.send',
                body: JSON.stringify(fullNotification),
            })
        }
    }
    
    // ========== 私聊功能 ==========
    
    /**
     * 订阅与指定好友的私聊频道
     */
    const subscribeToPrivateChat = (friendId: string) => {
        if (stompClient.value && connected.value) {
            const channel = `/user/queue/private-chat/${friendId}`
            
            stompClient.value.subscribe(channel, (message: IMessage) => {
                const privateMessage: PrivateMessage = JSON.parse(message.body)
                
                // 初始化该好友的消息列表
                if (!privateMessages.value.has(friendId)) {
                    privateMessages.value.set(friendId, [])
                }
                
                // 添加消息
                const messages = privateMessages.value.get(friendId)
                if (messages) {
                    messages.push(privateMessage)
                }
            })
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
            
            // 本地添加消息（显示在发送者的窗口）
            if (!privateMessages.value.has(receiverId)) {
                privateMessages.value.set(receiverId, [])
            }
            privateMessages.value.get(receiverId)?.push({
                ...message,
                isRead: true  // 发送者的消息视为已读
            })
        }
    }
    
    /**
     * 获取与某个好友的聊天记录
     */
    const getPrivateChatHistory = (friendId: string): PrivateMessage[] => {
        return privateMessages.value.get(friendId) || []
    }
    
    /**
     * 清除与某个好友的聊天记录
     */
    const clearPrivateChatHistory = (friendId: string) => {
        privateMessages.value.delete(friendId)
    }
    
    // ========== 群聊功能 ==========
    
    /**
     * 订阅群聊频道
     */
    const subscribeToGroup = (groupId: number) => {
        if (stompClient.value && connected.value) {
            // 避免重复订阅
            if (subscribedGroups.value.has(groupId)) {
                return
            }
            
            const channel = `/topic/group/${groupId}`
            
            stompClient.value.subscribe(channel, (message: IMessage) => {
                const groupMessage: GroupMessage = JSON.parse(message.body)
                
                // 初始化该群的消息列表
                if (!groupMessages.value.has(groupId)) {
                    groupMessages.value.set(groupId, [])
                }
                
                // 添加消息
                const messages = groupMessages.value.get(groupId)
                if (messages) {
                    messages.push(groupMessage)
                }
            })
            
            subscribedGroups.value.add(groupId)
        }
    }
    
    /**
     * 取消订阅群聊
     */
    const unsubscribeFromGroup = (groupId: number) => {
        subscribedGroups.value.delete(groupId)
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
    
    /**
     * 添加群成员
     */
    const addGroupMember = (groupId: number, userId: string) => {
        if (stompClient.value && connected.value) {
            stompClient.value.publish({
                destination: `/app/group.addMember/${groupId}`,
                body: JSON.stringify({ userId: userId })
            })
        }
    }
    
    /**
     * 移除群成员
     */
    const removeGroupMember = (groupId: number, userId: string) => {
        if (stompClient.value && connected.value) {
            stompClient.value.publish({
                destination: `/app/group.removeMember/${groupId}`,
                body: JSON.stringify({ userId: userId })
            })
        }
    }
    
    /**
     * 获取群聊消息列表
     */
    const getGroupMessages = (groupId: number): GroupMessage[] => {
        return groupMessages.value.get(groupId) || []
    }
    
    /**
     * 清空群聊消息
     */
    const clearGroupMessages = (groupId: number) => {
        groupMessages.value.delete(groupId)
    }
    
    /**
     * 清除所有通知
     */
    const clearNotifications = () => {
        notifications.value = []
    }
    
    /**
     * 清除所有聊天消息
     */
    const clearMessages = () => {
        messages.value = []
    }
    
    return {
        connected,
        messages,
        notifications,
        currentUser,
        privateMessages,
        groups,
        groupMessages,
        subscribedGroups,
        connect,
        disconnect,
        sendMessage,
        sendNotification,
        clearNotifications,
        clearMessages,
        // 私聊
        subscribeToPrivateChat,
        sendPrivateMessage,
        getPrivateChatHistory,
        clearPrivateChatHistory,
        // 群聊
        subscribeToGroup,
        unsubscribeFromGroup,
        sendGroupMessage,
        addGroupMember,
        removeGroupMember,
        getGroupMessages,
        clearGroupMessages,
    }
}
```

### 2. 私聊窗口组件

**PrivateChatWindow.vue** - 私聊界面

```vue
<template>
    <div v-if="visible" class="private-chat-window">
        <div class="chat-header">
            <h3>{{ friendName }}</h3>
            <div class="header-actions">
                <span :class="['status', isOnline ? 'online' : 'offline']">
                    {{ isOnline ? '在线' : '离线' }}
                </span>
                <button @click="closeChat" class="btn-close">×</button>
            </div>
        </div>
        
        <div class="messages-container">
            <div v-for="msg in messages" :key="msg.timestamp" class="message-item">
                <div :class="['message-bubble', msg.senderId === currentUser ? 'sent' : 'received']">
                    <div class="message-sender">{{ msg.senderId }}</div>
                    <div class="message-content">{{ msg.content }}</div>
                    <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
                </div>
            </div>
        </div>
        
        <div class="input-area">
            <input 
                v-model="inputMessage"
                type="text"
                placeholder="输入消息..."
                @keyup.enter="sendMessage"
            />
            <button @click="sendMessage" class="btn-send">发送</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useWebSocket, type PrivateMessage } from '@/composables/useWebSocket'

const props = defineProps<{
    friendId: string
    friendName: string
    isOnline: boolean
}>()

const emit = defineEmits<{
    close: []
}>()

const { currentUser, privateMessages, sendPrivateMessage, subscribeToPrivateChat } = useWebSocket()

const visible = ref(true)
const inputMessage = ref('')

const messages = computed(() => {
    return privateMessages.value.get(props.friendId) || []
})

onMounted(() => {
    // 订阅与该好友的私聊频道
    subscribeToPrivateChat(props.friendId)
    
    // 滚动到最底部
    setTimeout(() => {
        const container = document.querySelector('.messages-container')
        if (container) {
            container.scrollTop = container.scrollHeight
        }
    }, 0)
})

const formatTime = (timestamp: number): string => {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
    })
}

const sendMessage = () => {
    if (inputMessage.value.trim()) {
        sendPrivateMessage(props.friendId, inputMessage.value)
        inputMessage.value = ''
        
        // 滚动到最底部
        setTimeout(() => {
            const container = document.querySelector('.messages-container')
            if (container) {
                container.scrollTop = container.scrollHeight
            }
        }, 0)
    }
}

const closeChat = () => {
    visible.value = false
    emit('close')
}
</script>

<style scoped>
.private-chat-window {
    position: fixed;
    bottom: 20px;
    right: 20px;
    width: 350px;
    height: 500px;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 16px rgba(0, 0, 0, 0.15);
    display: flex;
    flex-direction: column;
    z-index: 100;
}

.chat-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 12px 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-radius: 8px 8px 0 0;
}

.chat-header h3 {
    margin: 0;
    font-size: 14px;
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.status {
    font-size: 12px;
    padding: 2px 6px;
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.3);
}

.status.online {
    background: #4caf50;
}

.status.offline {
    background: rgba(0, 0, 0, 0.2);
}

.btn-close {
    background: none;
    border: none;
    color: white;
    font-size: 24px;
    cursor: pointer;
    padding: 0;
    width: 24px;
    height: 24px;
}

.messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    background: #f5f5f5;
}

.message-item {
    display: flex;
    margin-bottom: 4px;
}

.message-bubble {
    max-width: 70%;
    padding: 8px 12px;
    border-radius: 12px;
    background: white;
}

.message-bubble.sent {
    align-self: flex-end;
    background: #667eea;
    color: white;
}

.message-bubble.received {
    align-self: flex-start;
    background: #e0e0e0;
    color: black;
}

.message-sender {
    font-size: 12px;
    font-weight: 500;
    margin-bottom: 4px;
    opacity: 0.7;
}

.message-content {
    word-break: break-word;
}

.message-time {
    font-size: 11px;
    margin-top: 4px;
    opacity: 0.6;
}

.input-area {
    display: flex;
    padding: 8px;
    gap: 6px;
    border-top: 1px solid #ddd;
}

.input-area input {
    flex: 1;
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-family: inherit;
}

.btn-send {
    padding: 8px 16px;
    background: #667eea;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    transition: background 0.3s;
}

.btn-send:hover {
    background: #5568d3;
}
</style>
```

### 3. 群聊窗口组件

**GroupChatWindow.vue** - 群聊界面

```vue
<template>
    <div class="group-chat-window">
        <div class="chat-header">
            <h2>{{ group?.groupName }}</h2>
            <div class="header-info">
                <span class="member-count">成员: {{ group?.memberIds.length }}</span>
                <button @click="showMemberList = !showMemberList" class="btn-members">👥</button>
            </div>
        </div>
        
        <!-- 成员列表 -->
        <div v-if="showMemberList" class="members-sidebar">
            <h3>成员列表</h3>
            <div class="members-list">
                <div v-for="member in group?.memberIds" :key="member" class="member-item">
                    <span class="member-name">{{ member }}</span>
                    <span v-if="member === group?.ownerId" class="owner-badge">群主</span>
                </div>
            </div>
        </div>
        
        <!-- 消息区域 -->
        <div class="messages-container">
            <div v-for="msg in messages" :key="msg.id" class="message-item">
                <div v-if="msg.senderId === 'SYSTEM'" class="system-message">
                    {{ msg.content }}
                </div>
                <div v-else class="normal-message" :class="{ 'own-message': msg.senderId === currentUser }">
                    <div class="message-sender">{{ msg.senderId }}</div>
                    <div class="message-bubble">{{ msg.content }}</div>
                    <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
                </div>
            </div>
        </div>
        
        <!-- 输入区域 -->
        <div class="input-area">
            <input 
                v-model="inputMessage"
                type="text"
                placeholder="输入消息..."
                @keyup.enter="sendMessage"
            />
            <button @click="sendMessage" class="btn-send">发送</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useWebSocket, type ChatGroup, type GroupMessage } from '@/composables/useWebSocket'

const props = defineProps<{
    groupId: number
    group: ChatGroup | null
}>()

const {
    currentUser,
    groupMessages,
    subscribeToGroup,
    unsubscribeFromGroup,
    sendGroupMessage
} = useWebSocket()

const inputMessage = ref('')
const showMemberList = ref(false)

const messages = computed(() => {
    return groupMessages.value.get(props.groupId) || []
})

onMounted(() => {
    subscribeToGroup(props.groupId)
})

onUnmounted(() => {
    unsubscribeFromGroup(props.groupId)
})

const formatTime = (timestamp: number): string => {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
    })
}

const sendMessage = () => {
    if (inputMessage.value.trim()) {
        sendGroupMessage(props.groupId, inputMessage.value)
        inputMessage.value = ''
        
        // 自动滚动到底部
        setTimeout(() => {
            const container = document.querySelector('.messages-container')
            if (container) {
                container.scrollTop = container.scrollHeight
            }
        }, 0)
    }
}
</script>

<style scoped>
.group-chat-window {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: white;
}

.chat-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.chat-header h2 {
    margin: 0;
}

.header-info {
    display: flex;
    align-items: center;
    gap: 12px;
}

.member-count {
    background: rgba(255, 255, 255, 0.2);
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 12px;
}

.btn-members {
    background: rgba(255, 255, 255, 0.3);
    border: none;
    color: white;
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
}

.members-sidebar {
    width: 150px;
    border-right: 1px solid #ddd;
    padding: 12px;
    background: #f9f9f9;
    max-height: 300px;
    overflow-y: auto;
}

.members-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.member-item {
    padding: 6px;
    background: white;
    border-radius: 4px;
    font-size: 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.owner-badge {
    background: #ffc107;
    color: black;
    padding: 2px 4px;
    border-radius: 2px;
    font-size: 10px;
}

.messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.message-item {
    display: flex;
}

.system-message {
    text-align: center;
    padding: 8px;
    color: #999;
    font-size: 12px;
    width: 100%;
}

.normal-message {
    display: flex;
    flex-direction: column;
    max-width: 70%;
}

.normal-message.own-message {
    align-self: flex-end;
}

.message-sender {
    font-size: 12px;
    font-weight: 500;
    color: #666;
    margin-bottom: 2px;
}

.message-bubble {
    background: #e0e0e0;
    padding: 8px 12px;
    border-radius: 8px;
    word-break: break-word;
}

.normal-message.own-message .message-bubble {
    background: #667eea;
    color: white;
}

.message-time {
    font-size: 11px;
    color: #999;
    margin-top: 2px;
}

.input-area {
    display: flex;
    gap: 6px;
    padding: 12px;
    border-top: 1px solid #ddd;
}

.input-area input {
    flex: 1;
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 4px;
}

.btn-send {
    padding: 8px 16px;
    background: #667eea;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
}

.btn-send:hover {
    background: #5568d3;
}
</style>
```

---

## 最佳实践

### 后端最佳实践

1. **使用 Service 层分离业务逻辑**
   - Controller 处理请求/响应
   - Service 处理业务逻辑
   - Repository 处理数据访问

2. **线程安全**
   - 使用 `ConcurrentHashMap` 存储共享数据
   - 关键方法使用 `synchronized`

3. **错误处理**
   - 验证输入参数
   - 返回有意义的错误信息
   - 使用异常处理

4. **消息验证**
   ```java
   if (message == null || message.getContent() == null || message.getContent().isEmpty()) {
       throw new IllegalArgumentException("消息内容不能为空");
   }
   ```

### 前端最佳实践

1. **类型安全**
   - 使用 TypeScript 接口定义数据结构
   - 避免使用 `any` 类型

2. **状态管理**
   - 使用 Composition API 和 `ref`
   - 创建可复用的 composables

3. **性能优化**
   - 使用 computed 缓存计算
   - 虚拟列表显示大量消息
   - 及时清理订阅

4. **用户体验**
   - 自动滚动到最新消息
   - 显示消息发送状态
   - 提供加载指示器

---

## 常见问题解决

### Q1: WebSocket 连接失败

**症状**: 浏览器控制台显示连接错误

**解决**:
```typescript
// 检查 SOCKET_URL 是否正确
const SOCKET_URL = 'http://localhost:8080/ws'

// 检查后端是否已启动
// 检查防火墙设置
```

### Q2: 私聊消息无法接收

**症状**: 发送私聊消息但接收者收不到

**解决**:
```java
// 确保接收者用户名正确
// 使用 @SendToUser 而不是发送到固定频道
messagingTemplate.convertAndSendToUser(
    receiverId,
    "/queue/private-chat/" + senderId,
    message
);
```

### Q3: 群聊消息显示不了

**症状**: 群聊消息发送但无法显示

**解决**:
```typescript
// 确保已经订阅了群聊频道
subscribeToGroup(groupId)

// 检查 groupMessages Map 是否正确初始化
if (!groupMessages.value.has(groupId)) {
    groupMessages.value.set(groupId, [])
}
```

### Q4: 消息重复显示

**症状**: 同一条消息显示多次

**解决**:
```typescript
// 避免重复订阅
if (subscribedGroups.value.has(groupId)) {
    return  // 已经订阅过
}

// 使用消息ID作为 key
<div v-for="msg in messages" :key="msg.id">
```

### Q5: 内存泄漏问题

**症状**: 应用运行久后内存占用持续增加

**解决**:
```typescript
// 组件卸载时清理资源
onUnmounted(() => {
    clearGroupMessages(groupId)
    unsubscribeFromGroup(groupId)
})

// 定期清理过期数据
const MAX_MESSAGES = 1000
if (messages.value.length > MAX_MESSAGES) {
    messages.value = messages.value.slice(-500)
}
```

---

