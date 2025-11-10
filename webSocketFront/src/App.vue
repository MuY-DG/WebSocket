<script setup lang="ts">
import { ref } from 'vue'
import ChatRoom from './components/ChatRoom.vue'
import NotificationPanel from './components/NotificationPanel.vue'
import { useWebSocket } from './composables/useWebSocket'

const {
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
} = useWebSocket()

const username = ref('')
const isLoggedIn = ref(false)

const handleLogin = () => {
  if (username.value.trim()) {
    connect(username.value.trim())
    isLoggedIn.value = true
  }
}

const handleLogout = () => {
  disconnect()
  isLoggedIn.value = false
  username.value = ''
  clearMessages()
  clearNotifications()
}
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <h1>🚀 WebSocket 示例应用</h1>
      <p class="subtitle">实时聊天 & 通知系统</p>
    </header>

    <!-- 登录表单 -->
    <div v-if="!isLoggedIn" class="login-container">
      <div class="login-card">
        <h2>欢迎</h2>
        <p>请输入您的用户名加入聊天</p>
        <form @submit.prevent="handleLogin" class="login-form">
          <input v-model="username" type="text" placeholder="输入用户名..." class="username-input" required />
          <button type="submit" class="login-button">加入聊天</button>
        </form>
      </div>
    </div>

    <!-- 主界面 -->
    <div v-else class="main-container">
      <div class="user-info">
        <span class="status-indicator" :class="{ connected }"></span>
        <span class="username">{{ currentUser }}</span>
        <button @click="handleLogout" class="logout-button">退出</button>
      </div>

      <div class="content-grid">
        <!-- 聊天室 -->
        <ChatRoom :messages="messages" :current-user="currentUser" :connected="connected" @send-message="sendMessage" />

        <!-- 通知面板 -->
        <NotificationPanel :notifications="notifications" :current-user="currentUser" :connected="connected"
          @send-notification="sendNotification" @clear-notifications="clearNotifications" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.app-header {
  text-align: center;
  color: white;
  margin-bottom: 30px;
}

.app-header h1 {
  font-size: 2.5rem;
  margin: 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.subtitle {
  font-size: 1.2rem;
  margin: 10px 0 0 0;
  opacity: 0.9;
}

/* 登录表单 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.login-card {
  background: white;
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-width: 400px;
  width: 100%;
  text-align: center;
}

.login-card h2 {
  color: #667eea;
  margin: 0 0 10px 0;
}

.login-card p {
  color: #666;
  margin: 0 0 30px 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.username-input {
  padding: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 1rem;
  transition: all 0.3s;
}

.username-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.login-button {
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
}

/* 主界面 */
.main-container {
  max-width: 1400px;
  margin: 0 auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
  background: rgba(255, 255, 255, 0.95);
  padding: 15px 25px;
  border-radius: 15px;
  margin-bottom: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ccc;
  transition: background 0.3s;
}

.status-indicator.connected {
  background: #4caf50;
  box-shadow: 0 0 10px #4caf50;
}

.username {
  font-weight: 600;
  color: #333;
  flex: 1;
}

.logout-button {
  padding: 8px 20px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.logout-button:hover {
  background: #d32f2f;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
