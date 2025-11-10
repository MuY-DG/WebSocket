package com.muybaby.websocket.controller;

import com.muybaby.websocket.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * 聊天控制器
 * 处理群聊消息
 */
@Controller
public class ChatController {

    /**
     * 发送聊天消息
     * 客户端发送到 /app/chat.sendMessage
     * 服务器广播到 /topic/public
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(System.currentTimeMillis());
        return chatMessage;
    }

    /**
     * 用户加入聊天室
     * 客户端发送到 /app/chat.addUser
     * 服务器广播到 /topic/public
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, 
                               SimpMessageHeaderAccessor headerAccessor) {
        // 在 WebSocket 会话中添加用户名
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        chatMessage.setTimestamp(System.currentTimeMillis());
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(chatMessage.getSender() + " 加入了聊天室！");
        return chatMessage;
    }
}
