package com.muybaby.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 发送者
     */
    private String sender;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        CHAT,      // 普通聊天消息
        JOIN,      // 用户加入
        LEAVE      // 用户离开
    }
}
