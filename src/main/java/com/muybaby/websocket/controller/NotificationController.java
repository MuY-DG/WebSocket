package com.muybaby.websocket.controller;

import com.muybaby.websocket.model.Notification;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 通知控制器
 * 处理点对点通知消息
 */
@Controller
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 广播通知给所有用户
     * 通过 WebSocket 发送
     */
    @MessageMapping("/notification.send")
    public void sendNotification(Notification notification) {
        notification.setTimestamp(System.currentTimeMillis());
        // 广播给所有订阅 /topic/notifications 的用户
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
}
