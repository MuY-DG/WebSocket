package com.muybaby.websocket.controller;

import com.muybaby.websocket.model.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 通知 REST 控制器
 * 提供 HTTP 接口发送通知
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationRestController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 通过 HTTP POST 发送通知
     */
    @PostMapping("/send")
    public Notification sendNotification(@RequestBody Notification notification) {
        notification.setTimestamp(System.currentTimeMillis());
        
        // 发送给特定用户
        messagingTemplate.convertAndSendToUser(
            notification.getRecipient(),
            "/queue/notifications",
            notification
        );
        
        return notification;
    }

    /**
     * 广播通知给所有用户
     */
    @PostMapping("/broadcast")
    public Notification broadcastNotification(@RequestBody Notification notification) {
        notification.setTimestamp(System.currentTimeMillis());
        
        // 广播给所有订阅 /topic/notifications 的用户
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        
        return notification;
    }
}
