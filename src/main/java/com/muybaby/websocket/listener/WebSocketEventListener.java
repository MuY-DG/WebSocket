package com.muybaby.websocket.listener;

import com.muybaby.websocket.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 事件监听器
 * 监听用户连接和断开事件
 */
@Slf4j
@Component
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 用户连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {

        log.info("收到新的 WebSocket 连接 {}",event);
    }

    /**
     * 用户断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        
        if (username != null) {
            log.info("用户断开连接: {}", username);
            
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSender(username);
            chatMessage.setContent(username + " 离开了聊天室！");
            chatMessage.setTimestamp(System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }
}
