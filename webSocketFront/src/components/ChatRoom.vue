<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import type { ChatMessage } from '../composables/useWebSocket'

const props = defineProps<{
    messages: ChatMessage[]
    currentUser: string
    connected: boolean
}>()

const emit = defineEmits<{
    sendMessage: [content: string]
}>()

const messageInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

// 发送消息
const handleSendMessage = () => {
    if (messageInput.value.trim() && props.connected) {
        emit('sendMessage', messageInput.value.trim())
        messageInput.value = ''
    }
}

// 格式化时间
const formatTime = (timestamp: number) => {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 自动滚动到底部
watch(
    () => props.messages.length,
    async () => {
        await nextTick()
        if (messagesContainer.value) {
            messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
        }
    }
)
</script>

<template>
    <div class="chat-room">
        <div class="chat-header">
            <h2>💬 群聊</h2>
            <span class="message-count">{{ messages.length }} 条消息</span>
        </div>

        <div ref="messagesContainer" class="messages-container">
            <div v-for="(message, index) in messages" :key="index" class="message-wrapper" :class="{
                'message-own': message.sender === currentUser && message.type === 'CHAT',
                'message-system': message.type !== 'CHAT',
            }">
                <!-- 系统消息 -->
                <div v-if="message.type !== 'CHAT'" class="system-message">
                    <span class="system-icon">{{ message.type === 'JOIN' ? '👋' : '👋' }}</span>
                    {{ message.content }}
                </div>

                <!-- 聊天消息 -->
                <div v-else class="chat-message">
                    <div class="message-header">
                        <span class="sender">{{ message.sender }}</span>
                        <span class="time">{{ formatTime(message.timestamp) }}</span>
                    </div>
                    <div class="message-content">{{ message.content }}</div>
                </div>
            </div>

            <div v-if="messages.length === 0" class="empty-state">
                <p>👋 还没有消息，发送第一条消息吧！</p>
            </div>
        </div>

        <div class="message-input-container">
            <input v-model="messageInput" type="text" placeholder="输入消息..." class="message-input"
                @keyup.enter="handleSendMessage" :disabled="!connected" />
            <button @click="handleSendMessage" class="send-button" :disabled="!connected || !messageInput.trim()">
                发送
            </button>
        </div>
    </div>
</template>

<style scoped>
.chat-room {
    background: white;
    border-radius: 20px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
    display: flex;
    flex-direction: column;
    height: 600px;
    overflow: hidden;
}

.chat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 25px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
}

.chat-header h2 {
    margin: 0;
    font-size: 1.5rem;
}

.message-count {
    font-size: 0.9rem;
    opacity: 0.9;
}

.messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #f5f5f5;
}

.message-wrapper {
    margin-bottom: 15px;
}

.message-system {
    display: flex;
    justify-content: center;
}

.system-message {
    background: rgba(102, 126, 234, 0.1);
    color: #667eea;
    padding: 8px 16px;
    border-radius: 20px;
    font-size: 0.9rem;
    display: inline-block;
}

.system-icon {
    margin-right: 5px;
}

.chat-message {
    background: white;
    padding: 12px 16px;
    border-radius: 12px;
    max-width: 70%;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

.message-own .chat-message {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    margin-left: auto;
}

.message-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 5px;
    gap: 10px;
}

.sender {
    font-weight: 600;
    font-size: 0.9rem;
}

.message-own .sender {
    color: rgba(255, 255, 255, 0.9);
}

.time {
    font-size: 0.75rem;
    opacity: 0.6;
}

.message-content {
    word-wrap: break-word;
    line-height: 1.5;
}

.empty-state {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    color: #999;
    text-align: center;
}

.message-input-container {
    display: flex;
    gap: 10px;
    padding: 20px;
    background: white;
    border-top: 1px solid #e0e0e0;
}

.message-input {
    flex: 1;
    padding: 12px 16px;
    border: 2px solid #e0e0e0;
    border-radius: 25px;
    font-size: 1rem;
    transition: border-color 0.3s;
}

.message-input:focus {
    outline: none;
    border-color: #667eea;
}

.message-input:disabled {
    background: #f5f5f5;
    cursor: not-allowed;
}

.send-button {
    padding: 12px 30px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border: none;
    border-radius: 25px;
    font-weight: 600;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
}

.send-button:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.send-button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
    width: 6px;
}

.messages-container::-webkit-scrollbar-track {
    background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
    background: #999;
}
</style>
