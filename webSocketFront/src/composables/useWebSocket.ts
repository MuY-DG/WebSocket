import { ref, onMounted, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Client, type IMessage } from '@stomp/stompjs'

/**
 * WebSocket 服务
 * 使用 STOMP 协议
 */
export interface ChatMessage {
    type: 'CHAT' | 'JOIN' | 'LEAVE'
    content: string
    sender: string
    timestamp: number
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

    // WebSocket 服务器地址
    const SOCKET_URL = 'http://localhost:8080/ws'

    /**
     * 连接 WebSocket
     */
    const connect = (username: string) => {
        currentUser.value = username

        // 创建 STOMP 客户端
        const client = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            debug: (str) => {
                console.log('STOMP Debug:', str)
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        })

        // 连接成功回调
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

        // 连接错误回调
        client.onStompError = (frame) => {
            console.error('STOMP 错误:', frame)
            connected.value = false
        }

        // 激活连接
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

    /**
     * 清除通知
     */
    const clearNotifications = () => {
        notifications.value = []
    }

    /**
     * 清除消息
     */
    const clearMessages = () => {
        messages.value = []
    }

    return {
        connected,
        messages,
        notifications,
        currentUser,
        connect,
        disconnect,
        sendMessage,
        sendNotification,
        clearNotifications,
        clearMessages,
    }
}
