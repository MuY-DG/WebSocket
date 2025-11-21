/**
 * 应用类型定义
 */

// ==================== 聊天消息 ====================

export interface ChatMessage {
    type: 'CHAT' | 'JOIN' | 'LEAVE'
    content: string
    sender: string
    timestamp: number
}

// ==================== 通知 ====================

export interface Notification {
    title: string
    message: string
    type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
    recipient: string
    timestamp: number
}

// ==================== 好友系统 ====================

export interface Friend {
    id: number
    userId: string
    friendId: string
    friendName: string
    status: FriendStatus
    createdAt: number
    updatedAt: number
}

export type FriendStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export interface FriendRequest {
    id: number
    senderId: string
    receiverId: string
    message: string
    status: RequestStatus
    createdAt: number
    updatedAt: number
}

export type RequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

// ==================== 私聊 ====================

export interface PrivateMessage {
    id?: number
    senderId: string
    receiverId: string
    content: string
    timestamp: number
    isRead: boolean
}

// ==================== 群聊 ====================

export interface ChatGroup {
    id: number
    groupName: string
    description: string
    ownerId: string
    avatarUrl?: string
    memberIds: string[]
    createdAt: number
    updatedAt: number
}

export interface GroupMessage {
    id: number
    groupId: number
    senderId: string
    content: string
    timestamp: number
    status: MessageStatus
}

export type MessageStatus = 'SENT' | 'DELIVERED' | 'READ'

export interface GroupMember {
    id: number
    groupId: number
    userId: string
    role: 'admin' | 'member'
    joinedAt: number
    status: 'active' | 'inactive'
}

// ==================== API 请求/响应 ====================

export interface ApiResponse<T> {
    code: number
    message: string
    data: T
}

export interface CreateGroupRequest {
    groupName: string
    description: string
    ownerId: string
}

export interface UpdateGroupRequest {
    groupName?: string
    description?: string
}

export interface SendFriendRequestPayload {
    senderId: string
    receiverId: string
    message: string
}

export interface AddMemberPayload {
    userId: string
}

// ==================== WebSocket 事件 ====================

export interface WebSocketEvent<T = any> {
    type: string
    data: T
    timestamp: number
}

export interface UserStatusEvent {
    userId: string
    status: 'online' | 'offline'
    timestamp: number
}
