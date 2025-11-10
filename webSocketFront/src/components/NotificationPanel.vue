<script setup lang="ts">
import { ref } from 'vue'
import type { Notification } from '../composables/useWebSocket'

const props = defineProps<{
    notifications: Notification[]
    currentUser: string
    connected: boolean
}>()

const emit = defineEmits<{
    sendNotification: [notification: Omit<Notification, 'timestamp'>]
    clearNotifications: []
}>()

const notificationForm = ref({
    recipient: '',
    title: '',
    message: '',
    type: 'INFO' as Notification['type'],
})

// 发送通知
const handleSendNotification = () => {
    if (
        notificationForm.value.title.trim() &&
        notificationForm.value.message.trim() &&
        props.connected
    ) {
        emit('sendNotification', {
            recipient: 'all', // 广播给所有用户
            title: notificationForm.value.title.trim(),
            message: notificationForm.value.message.trim(),
            type: notificationForm.value.type,
        })

        // 重置表单
        notificationForm.value = {
            recipient: '',
            title: '',
            message: '',
            type: 'INFO',
        }
    }
}

// 格式化时间
const formatTime = (timestamp: number) => {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// 获取通知图标
const getNotificationIcon = (type: Notification['type']) => {
    const icons = {
        INFO: 'ℹ️',
        SUCCESS: '✅',
        WARNING: '⚠️',
        ERROR: '❌',
    }
    return icons[type]
}

// 获取通知颜色类
const getNotificationClass = (type: Notification['type']) => {
    return `notification-${type.toLowerCase()}`
}
</script>

<template>
    <div class="notification-panel">
        <div class="panel-header">
            <h2>🔔 通知中心</h2>
            <button @click="emit('clearNotifications')" class="clear-button" v-if="notifications.length > 0">
                清空
            </button>
        </div>

        <div class="notifications-container">
            <div v-for="(notification, index) in notifications" :key="index" class="notification-item"
                :class="getNotificationClass(notification.type)">
                <div class="notification-icon">{{ getNotificationIcon(notification.type) }}</div>
                <div class="notification-body">
                    <div class="notification-title">{{ notification.title }}</div>
                    <div class="notification-message">{{ notification.message }}</div>
                    <div class="notification-meta">
                        <span class="notification-time">{{ formatTime(notification.timestamp) }}</span>
                    </div>
                </div>
            </div>

            <div v-if="notifications.length === 0" class="empty-state">
                <p>📭 暂无通知</p>
            </div>
        </div>

        <div class="notification-form">
            <h3>📢 发送系统通知（广播给所有用户）</h3>
            <form @submit.prevent="handleSendNotification">
                <div class="form-group">
                    <label>类型</label>
                    <select v-model="notificationForm.type" :disabled="!connected">
                        <option value="INFO">信息</option>
                        <option value="SUCCESS">成功</option>
                        <option value="WARNING">警告</option>
                        <option value="ERROR">错误</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>标题</label>
                    <input v-model="notificationForm.title" type="text" placeholder="通知标题..." :disabled="!connected" />
                </div>

                <div class="form-group">
                    <label>内容</label>
                    <textarea v-model="notificationForm.message" placeholder="通知内容..." rows="3"
                        :disabled="!connected"></textarea>
                </div>

                <button type="submit" class="submit-button" :disabled="!connected ||
                    !notificationForm.title.trim() ||
                    !notificationForm.message.trim()
                    ">
                    📣 广播通知
                </button>
            </form>
        </div>
    </div>
</template>

<style scoped>
.notification-panel {
    background: white;
    border-radius: 20px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
    display: flex;
    flex-direction: column;
    height: 600px;
    overflow: hidden;
}

.panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 25px;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    color: white;
}

.panel-header h2 {
    margin: 0;
    font-size: 1.5rem;
}

.clear-button {
    padding: 6px 16px;
    background: rgba(255, 255, 255, 0.2);
    color: white;
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 20px;
    cursor: pointer;
    font-size: 0.85rem;
    transition: background 0.2s;
}

.clear-button:hover {
    background: rgba(255, 255, 255, 0.3);
}

.notifications-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #f5f5f5;
}

.notification-item {
    display: flex;
    gap: 12px;
    background: white;
    padding: 15px;
    border-radius: 12px;
    margin-bottom: 12px;
    border-left: 4px solid;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
    animation: slideIn 0.3s ease;
}

@keyframes slideIn {
    from {
        opacity: 0;
        transform: translateX(20px);
    }

    to {
        opacity: 1;
        transform: translateX(0);
    }
}

.notification-info {
    border-left-color: #2196f3;
}

.notification-success {
    border-left-color: #4caf50;
}

.notification-warning {
    border-left-color: #ff9800;
}

.notification-error {
    border-left-color: #f44336;
}

.notification-icon {
    font-size: 1.5rem;
}

.notification-body {
    flex: 1;
}

.notification-title {
    font-weight: 600;
    color: #333;
    margin-bottom: 5px;
}

.notification-message {
    color: #666;
    font-size: 0.9rem;
    margin-bottom: 8px;
}

.notification-meta {
    display: flex;
    gap: 12px;
    font-size: 0.75rem;
    color: #999;
}

.notification-recipient {
    font-style: italic;
}

.empty-state {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    color: #999;
    text-align: center;
}

.notification-form {
    padding: 20px;
    background: white;
    border-top: 1px solid #e0e0e0;
}

.notification-form h3 {
    margin: 0 0 15px 0;
    font-size: 1.1rem;
    color: #333;
}

.form-group {
    margin-bottom: 12px;
}

.form-group label {
    display: block;
    margin-bottom: 5px;
    font-size: 0.85rem;
    font-weight: 600;
    color: #666;
}

.form-group input,
.form-group select,
.form-group textarea {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    font-size: 0.9rem;
    transition: border-color 0.3s;
    box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
    outline: none;
    border-color: #f093fb;
}

.form-group input:disabled,
.form-group select:disabled,
.form-group textarea:disabled {
    background: #f5f5f5;
    cursor: not-allowed;
}

.submit-button {
    width: 100%;
    padding: 10px;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    color: white;
    border: none;
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
}

.submit-button:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(240, 147, 251, 0.4);
}

.submit-button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
}

/* 滚动条样式 */
.notifications-container::-webkit-scrollbar {
    width: 6px;
}

.notifications-container::-webkit-scrollbar-track {
    background: transparent;
}

.notifications-container::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 3px;
}

.notifications-container::-webkit-scrollbar-thumb:hover {
    background: #999;
}
</style>
