package com.muybaby.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知消息实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String message;
    
    /**
     * 通知类型
     */
    private NotificationType type;
    
    /**
     * 接收者（用户名）
     */
    private String recipient;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        INFO,      // 信息
        SUCCESS,   // 成功
        WARNING,   // 警告
        ERROR      // 错误
    }
}
